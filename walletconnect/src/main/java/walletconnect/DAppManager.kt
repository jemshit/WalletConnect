/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect

import kotlinx.coroutines.launch
import walletconnect.core.DApp
import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.adapter.JsonAdapter
import walletconnect.core.requests.eth.EthSign
import walletconnect.core.requests.eth.EthTransaction
import walletconnect.core.requests.eth.SignType
import walletconnect.core.session.callback.RequestCallback
import walletconnect.core.session.callback.SessionCallback
import walletconnect.core.session.model.SessionRequest
import walletconnect.core.session.model.SessionResponse
import walletconnect.core.session.model.SessionUpdate
import walletconnect.core.session.model.json_rpc.*
import walletconnect.core.session_state.SessionStore
import walletconnect.core.session_state.model.SessionState
import walletconnect.core.socket.Socket
import walletconnect.core.socket.model.SocketMessage
import walletconnect.core.socket.model.SocketMessageType
import walletconnect.core.toFailure
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.EmptyLogger
import java.lang.reflect.Type

class DAppManager(socket: Socket,
                  sessionStore: SessionStore,
                  jsonAdapter: JsonAdapter,
                  dispatcherProvider: DispatcherProvider,
                  logger: Logger = EmptyLogger)
    : DApp, WalletConnectCore(isDApp = true, socket, sessionStore, jsonAdapter, dispatcherProvider, logger) {

    private val LogTag = "DApp"

    // No need for ConcurrentHashMap, because we don't iterate while adding/removing
    private val messageIdCallbackMap: MutableMap<Long, (RequestCallback) -> Unit> = mutableMapOf()

    // region Session
    override fun sendSessionRequest(chainId: Int?) {
        if (!initialized.get()) {
            logger.error(LogTag, "#sendSessionRequest(): !initialized")
            return
        }
        if (sessionApproved.get()) {
            return
        }

        logger.info(LogTag, "#sendSessionRequest()")
        coroutineScope.launch {
            val model = SessionRequest(peerId = initialState.myPeerId,
                                       peerMeta = initialState.myPeerMeta,
                                       chainId = chainId)
            val messageId = generateMessageId()
            val payload = JsonRpcRequest(id = messageId,
                                         method = SessionRpcMethod.Request,
                                         params = listOf(model))
            val payloadAsJson = serializePayload(payload,
                                                 getRequestType(SessionRequest::class.java))
                                ?: return@launch

            val encryptedPayload = encryptPayload(payloadAsJson)
                                   ?: return@launch
            val message = SocketMessage(topic = initialState.connectionParams.topic,
                                        type = SocketMessageType.Pub,
                                        payload = encryptedPayload)

            // store for dApp to check response from wallet
            sessionRequestId = messageId
            publish(message,
                    queueIfDisconnected = true)

            // notify dApp
            callback(SessionCallback.SessionRequested(
                    messageId,
                    chainId,
                    initialState.myPeerId,
                    initialState.myPeerMeta
            ))
        }
    }
    // endregion

    // region Custom Request/Response
    override suspend fun sendRequest(method: JsonRpcMethod,
                                     data: List<Any>,
                                     itemType: Type)
            : Long? {
        if (!initialized.get()) {
            logger.error(LogTag, "#sendRequest(): !initialized")
            return null
        }
        if (!sessionApproved.get()) {
            logger.warning(LogTag, "#sendRequest(): session isn't approved")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#sendRequest(): session isn't approved"))
            return null
        }

        // extra validation could be done on [data] based on [method] (is address in approved accounts)

        val messageId = generateMessageId()
        val payload = JsonRpcRequest(id = messageId,
                                     method = method,
                                     params = data)

        val finalMessageId: Long? = encryptPayloadAndPublish(messageId,
                                                             payload,
                                                             getRequestType(itemType),
                                                             queueIfDisconnected = true)

        // notify dApp
        if (finalMessageId != null) {
            logger.info(LogTag, "SendRequest($messageId), $method")

            // save for dApp
            messageMethodMap[messageId] = method

            try {
                when (method) {
                    is CustomRpcMethod -> {
                        callback(RequestCallback.CustomRequested(messageId,
                                                                 method.value,
                                                                 data))
                    }

                    is EthRpcMethod -> {
                        when (method) {
                            EthRpcMethod.Sign -> {
                                callback(RequestCallback.EthSignRequested(messageId,
                                                                          EthSign(address = data[0] as String,
                                                                                  message = data[1] as String,
                                                                                  type = SignType.Sign)))
                            }
                            EthRpcMethod.PersonalSign -> {
                                callback(RequestCallback.EthSignRequested(messageId,
                                                                          EthSign(address = data[1] as String,
                                                                                  message = data[0] as String,
                                                                                  type = SignType.PersonalSign)))
                            }
                            EthRpcMethod.SignTypedData -> {
                                callback(RequestCallback.EthSignTypedDataRequested(messageId,
                                                                                   data[0] as String,
                                                                                   data[1]))
                            }

                            EthRpcMethod.SignTransaction -> {
                                callback(RequestCallback.EthSignTxRequested(messageId,
                                                                            data[0] as EthTransaction))
                            }
                            EthRpcMethod.SendRawTransaction -> {
                                callback(RequestCallback.EthSendRawTxRequested(messageId,
                                                                               data[0] as String))
                            }
                            EthRpcMethod.SendTransaction -> {
                                callback(RequestCallback.EthSendTxRequested(messageId,
                                                                            data[0] as EthTransaction))
                            }
                        }
                    }

                    else -> {
                        logger.error(LogTag, "sendRequest() has unexpected method:$method. " +
                                             "Must be one of [CustomRpcMethod, EthRpcMethod]")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "sendRequest() has unexpected method:$method. " +
                                                          "Must be one of [CustomRpcMethod, EthRpcMethod]"))
                    }
                }
            } catch (error: Exception) {
                logger.error(LogTag, error.stackTraceToString())
                failureCallback(Failure(type = FailureType.InvalidRequest,
                                        message = error.stackTraceToString()))
            }
        }

        return finalMessageId
    }

    override fun sendRequestAsync(method: JsonRpcMethod,
                                  data: List<Any>,
                                  itemType: Type,
                                  onRequested: (() -> Unit)?,
                                  onRequestError: ((String?) -> Unit)?,
                                  onCallback: ((RequestCallback) -> Unit)?) {
        if (!initialized.get()) {
            logger.error(LogTag, "#sendRequest(): !initialized")
            onRequestError?.invoke("!initialized")
            return
        }
        if (!sessionApproved.get()) {
            logger.warning(LogTag, "#sendRequest(): session isn't approved")
            onRequestError?.invoke("Session isn't approved")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#sendRequest(): session isn't approved"))
            return
        }

        // extra validation could be done on [data] based on [method] (is address in approved accounts)

        coroutineScope.launch {
            val messageId = generateMessageId()
            if (onCallback != null) {
                // assign this before [encryptPayloadAndPublish], so callback will be in Map if response arrives very fast
                // if on next lines we invoke onRequestError, then remove from Map
                messageIdCallbackMap[messageId] = onCallback
            }
            val payload = JsonRpcRequest(id = messageId,
                                         method = method,
                                         params = data)

            val finalMessageId: Long? = encryptPayloadAndPublish(messageId,
                                                                 payload,
                                                                 getRequestType(itemType),
                                                                 queueIfDisconnected = true)

            // notify dApp
            if (finalMessageId == null) {
                messageIdCallbackMap.remove(messageId)
                onRequestError?.invoke("Couldn't encrypt or publish")
            } else {
                logger.info(LogTag, "SendRequest($messageId), $method")

                // save for dApp
                messageMethodMap[messageId] = method

                try {
                    when (method) {
                        is CustomRpcMethod -> {
                            onRequested?.invoke()
                            callback(RequestCallback.CustomRequested(messageId,
                                                                     method.value,
                                                                     data))
                        }

                        is EthRpcMethod -> {
                            onRequested?.invoke()

                            when (method) {
                                EthRpcMethod.Sign -> {
                                    callback(RequestCallback.EthSignRequested(messageId,
                                                                              EthSign(address = data[0] as String,
                                                                                      message = data[1] as String,
                                                                                      type = SignType.Sign)))
                                }
                                EthRpcMethod.PersonalSign -> {
                                    callback(RequestCallback.EthSignRequested(messageId,
                                                                              EthSign(address = data[1] as String,
                                                                                      message = data[0] as String,
                                                                                      type = SignType.PersonalSign)))
                                }
                                EthRpcMethod.SignTypedData -> {
                                    callback(RequestCallback.EthSignTypedDataRequested(messageId,
                                                                                       data[0] as String,
                                                                                       data[1]))
                                }

                                EthRpcMethod.SignTransaction -> {
                                    callback(RequestCallback.EthSignTxRequested(messageId,
                                                                                data[0] as EthTransaction))
                                }
                                EthRpcMethod.SendRawTransaction -> {
                                    callback(RequestCallback.EthSendRawTxRequested(messageId,
                                                                                   data[0] as String))
                                }
                                EthRpcMethod.SendTransaction -> {
                                    callback(RequestCallback.EthSendTxRequested(messageId,
                                                                                data[0] as EthTransaction))
                                }
                            }
                        }

                        else -> {
                            logger.error(LogTag, "sendRequest() has unexpected method:$method. " +
                                                 "Must be one of [CustomRpcMethod, EthRpcMethod]")
                            messageIdCallbackMap.remove(messageId)
                            onRequestError?.invoke("sendRequest() has unexpected method:$method. " +
                                                   "Must be one of [CustomRpcMethod, EthRpcMethod]")
                            failureCallback(Failure(type = FailureType.InvalidRequest,
                                                    message = "sendRequest() has unexpected method:$method. " +
                                                              "Must be one of [CustomRpcMethod, EthRpcMethod]"))
                        }
                    }
                } catch (error: Exception) {
                    logger.error(LogTag, error.stackTraceToString())
                    messageIdCallbackMap.remove(messageId)
                    onRequestError?.invoke(error.stackTraceToString())
                    failureCallback(Failure(type = FailureType.InvalidRequest,
                                            message = error.stackTraceToString()))
                }
            }
        }
    }

    // endregion

    // region Incoming Messages
    override suspend fun handleJsonRpcRequest(decryptedPayload: String,
                                              method: String?,
                                              messageId: Long) {
        logger.debug(LogTag,
                     "#handleJsonRpcRequest(). method:$method, id:$messageId, decryptedPayload:$decryptedPayload")

        try {
            when (method) {
                null -> {
                    logger.error(LogTag, "#handleJsonRpcRequest(): 'method' is not String")
                    messageIdCallbackMap.remove(messageId)
                    failureCallback(Failure(type = FailureType.DeserializeSessionMessage))
                    reportInvalidSocketMessage(messageId,
                                               JsonRpcErrorData(RpcErrorCode.RequestMethodNotAvailable,
                                                                "'method' is not String"))
                    return
                }

                SessionRpcMethod.Update.value -> {
                    val payload: JsonRpcRequest<SessionUpdate> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(SessionUpdate::class.java)
                    ) ?: return
                    logger.debug(LogTag, "Received SessionUpdate(${payload.params[0].approved})")

                    // session ended by peer
                    if (!payload.params[0].approved) {
                        sessionApproved.set(false)
                        messageIdCallbackMap.remove(messageId)
                        callback(SessionCallback.SessionDeleted(byMe = false))
                        closeAsync(deleteLocal = true,
                                   deleteRemote = false)
                        return
                    }
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "Session wasn't approved, but wallet tries to update!")
                        messageIdCallbackMap.remove(messageId)
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "Session wasn't approved, but wallet tries to update!"))
                        return
                    }

                    // update session
                    sessionApproved.set(true)
                    // update [sessionState] of dApp
                    this.sessionState = sessionState?.copy(
                            chainId = payload.params[0].chainId,
                            accounts = payload.params[0].accounts,

                            updatedAt = System.currentTimeMillis()
                    )
                    sessionStore.persist(initialState.connectionParams.topic,
                                         sessionState!!)

                    // notify dApp
                    messageIdCallbackMap.remove(messageId)
                    callback(SessionCallback.SessionUpdated(
                            messageId = messageId,
                            chainId = payload.params[0].chainId,
                            accounts = payload.params[0].accounts
                    ))
                }

                else -> {
                    // NoOp
                }
            }
        } catch (error: Exception) {
            // List<Any>[0] might throw
            logger.error(LogTag, "#handleJsonRpcRequest(): ${error.stackTraceToString()}")
            messageIdCallbackMap.remove(messageId)
            failureCallback(error.toFailure(type = FailureType.DeserializeSessionMessage))
            reportInvalidSocketMessage(messageId,
                                       JsonRpcErrorData(RpcErrorCode.InvalidJson,
                                                        "Error while trying to process the message content!"))
        }
    }

    override suspend fun handleJsonRpcResponse(decryptedPayload: String,
                                               messageId: Long) {
        logger.debug(LogTag, "#handleJsonRpcResponse(id:$messageId, payload:$decryptedPayload)")

        // 1)dApp sends request and closes locally (sessionRequestId is lost) 2)wallet receives 3)dApp opens again
        // 4)wallet approves 5)dApp should know that this is SessionResponse
        val sessionResponsePayload: JsonRpcResponse<SessionResponse>? = deserializePayload(
                decryptedPayload,
                messageId,
                getResponseType(SessionResponse::class.java),
                ignoreError = true
        )
        if (messageId == sessionRequestId || sessionResponsePayload != null) {
            logger.debug(LogTag, "Received SessionResponse(${sessionResponsePayload!!.result.approved})")

            // because of above explained case
            sessionRequestId = messageId

            // some libs reject session using [JsonRpcError], some using [JsonRpcResponse]
            if (!sessionResponsePayload.result.approved) {
                sessionApproved.set(false)
                // notify dApp
                messageIdCallbackMap.remove(messageId)
                callback(SessionCallback.SessionRejected(null))
                closeAsync(deleteLocal = true,
                           deleteRemote = false)
                return
            }

            // session approved
            // update [sessionState] of dApp
            sessionApproved.set(true)
            this.sessionState = SessionState(
                    connectionParams = initialState.connectionParams,

                    myPeerId = initialState.myPeerId,
                    myPeerMeta = initialState.myPeerMeta,
                    remotePeerId = sessionResponsePayload.result.peerId,
                    remotePeerMeta = sessionResponsePayload.result.peerMeta,

                    chainId = sessionResponsePayload.result.chainId,
                    accounts = sessionResponsePayload.result.accounts,

                    updatedAt = System.currentTimeMillis()
            )
            sessionStore.persist(initialState.connectionParams.topic,
                                 sessionState!!)

            // notify dApp
            messageIdCallbackMap.remove(messageId)
            callback(SessionCallback.SessionApproved(
                    messageId = messageId,
                    chainId = sessionResponsePayload.result.chainId,
                    accounts = sessionResponsePayload.result.accounts,
                    remotePeerId = sessionResponsePayload.result.peerId,
                    remotePeerMeta = sessionResponsePayload.result.peerMeta
            ))
            return
        }

        if (!sessionApproved.get()) {
            logger.error(LogTag, "handleJsonRpcResponse() but !sessionApproved")
            messageIdCallbackMap.remove(messageId)
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "handleJsonRpcResponse() but !sessionApproved"))
            return
        }

        when (val method = messageMethodMap[messageId]) {
            is CustomRpcMethod -> {
                val payload: JsonRpcResponse<Any> = deserializePayload(
                        decryptedPayload,
                        messageId,
                        getResponseType(Any::class.java)
                ) ?: return

                // notify dApp
                messageIdCallbackMap[messageId]?.invoke(RequestCallback.CustomResponse(messageId,
                                                                                       payload.result))
                messageIdCallbackMap.remove(messageId)
                callback(RequestCallback.CustomResponse(messageId,
                                                        payload.result))
            }
            null -> {
                // messageMethodMap[messageId] data is somehow lost (activity/process death?)
                val payload: JsonRpcResponse<Any> = deserializePayload(
                        decryptedPayload,
                        messageId,
                        getResponseType(Any::class.java)
                ) ?: return

                // notify dApp
                messageIdCallbackMap[messageId]?.invoke(RequestCallback.CustomResponse(messageId,
                                                                                       payload.result))
                messageIdCallbackMap.remove(messageId)
                callback(RequestCallback.CustomResponse(messageId,
                                                        payload.result))
            }

            is EthRpcMethod -> {
                when (method) {
                    EthRpcMethod.Sign -> {
                        val payload: JsonRpcResponse<String> = deserializePayload(
                                decryptedPayload,
                                messageId,
                                getResponseType(String::class.java)
                        ) ?: return

                        // notify dApp
                        messageIdCallbackMap[messageId]?.invoke(RequestCallback.EthSignResponse(messageId,
                                                                                                payload.result))
                        messageIdCallbackMap.remove(messageId)
                        callback(RequestCallback.EthSignResponse(messageId,
                                                                 payload.result))
                    }
                    EthRpcMethod.PersonalSign -> {
                        val payload: JsonRpcResponse<String> = deserializePayload(
                                decryptedPayload,
                                messageId,
                                getResponseType(String::class.java)
                        ) ?: return

                        // notify dApp
                        messageIdCallbackMap[messageId]?.invoke(RequestCallback.EthSignResponse(messageId,
                                                                                                payload.result))
                        messageIdCallbackMap.remove(messageId)
                        callback(RequestCallback.EthSignResponse(messageId,
                                                                 payload.result))
                    }
                    EthRpcMethod.SignTypedData -> {
                        val payload: JsonRpcResponse<String> = deserializePayload(
                                decryptedPayload,
                                messageId,
                                getResponseType(String::class.java)
                        ) ?: return

                        // notify dApp
                        messageIdCallbackMap[messageId]?.invoke(RequestCallback.EthSignResponse(messageId,
                                                                                                payload.result))
                        messageIdCallbackMap.remove(messageId)
                        callback(RequestCallback.EthSignResponse(messageId,
                                                                 payload.result))
                    }

                    EthRpcMethod.SignTransaction -> {
                        val payload: JsonRpcResponse<String> = deserializePayload(
                                decryptedPayload,
                                messageId,
                                getResponseType(String::class.java)
                        ) ?: return

                        // notify dApp
                        messageIdCallbackMap[messageId]?.invoke(RequestCallback.EthSignTxResponse(messageId,
                                                                                                  payload.result))
                        messageIdCallbackMap.remove(messageId)
                        callback(RequestCallback.EthSignTxResponse(messageId,
                                                                   payload.result))
                    }
                    EthRpcMethod.SendRawTransaction -> {
                        val payload: JsonRpcResponse<Any?> = deserializePayload(
                                decryptedPayload,
                                messageId,
                                getResponseType(Any::class.java)
                        ) ?: return

                        // notify dApp
                        messageIdCallbackMap[messageId]?.invoke(RequestCallback.EthSendRawTxResponse(messageId,
                                                                                                     payload.result as? String?))
                        messageIdCallbackMap.remove(messageId)
                        callback(RequestCallback.EthSendRawTxResponse(messageId,
                                                                      payload.result as? String?))
                    }
                    EthRpcMethod.SendTransaction -> {
                        val payload: JsonRpcResponse<Any?> = deserializePayload(
                                decryptedPayload,
                                messageId,
                                getResponseType(Any::class.java)
                        ) ?: return

                        // notify dApp
                        messageIdCallbackMap[messageId]?.invoke(RequestCallback.EthSendTxResponse(messageId,
                                                                                                  payload.result as? String?))
                        messageIdCallbackMap.remove(messageId)
                        callback(RequestCallback.EthSendTxResponse(messageId,
                                                                   payload.result as? String?))
                    }
                }
            }

            else -> {
                logger.error(LogTag, "Unexpected method($method) in #handleJsonRpcResponse($messageId)")
                messageIdCallbackMap.remove(messageId)
                failureCallback(Failure(type = FailureType.InvalidResponse,
                                        message = "Unexpected method($method) in #handleJsonRpcResponse)($messageId)"))
            }
        }
    }

    override suspend fun handleJsonRpcError(decryptedPayload: String,
                                            messageId: Long) {
        logger.debug(LogTag, "#handleJsonRpcError(id:$messageId, payload:$decryptedPayload)")
        val payload: JsonRpcError = deserializePayload(
                decryptedPayload,
                messageId,
                JsonRpcError::class.java
        ) ?: return

        // session rejected by peer
        if (messageId == sessionRequestId) {
            logger.debug(LogTag, "Received SessionReject")
            sessionApproved.set(false)
            // notify dApp
            messageIdCallbackMap.remove(messageId)
            callback(SessionCallback.SessionRejected(payload.error.message))
            closeAsync(deleteLocal = true,
                       deleteRemote = false)
            return
        }

        // rejected requests
        if (sessionApproved.get()) {
            // there is chance where this might be null but still it should be RequestRejected.
            if (messageMethodMap[messageId] != null) {
                // notify dApp
                messageIdCallbackMap[messageId]?.invoke(RequestCallback.RequestRejected(messageId,
                                                                                        payload.error))
                messageIdCallbackMap.remove(messageId)
                callback(RequestCallback.RequestRejected(messageId,
                                                         payload.error))
                return
            }
        } else {
            logger.warning(LogTag, "Session not approved yet but received JsonRpcError")
        }

        /** messages from [reportInvalidSocketMessage] */
        messageIdCallbackMap.remove(messageId)
        failureCallback(Failure(type = FailureType.SessionError,
                                message = payload.error.toString()))
    }

    override suspend fun handleCustom(decryptedPayload: String,
                                      messageId: Long) {
        logger.debug(LogTag, "#handleCustom(id:$messageId, payload:$decryptedPayload)")

        messageIdCallbackMap[messageId]?.invoke(RequestCallback.CustomResponse(messageId,
                                                                               decryptedPayload))
        messageIdCallbackMap.remove(messageId)
        callback(RequestCallback.CustomResponse(messageId,
                                                decryptedPayload))

    }
    // endregion

    override fun onClosing() {
        messageIdCallbackMap.clear()
    }

}
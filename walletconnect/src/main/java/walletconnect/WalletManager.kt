/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect

import kotlinx.coroutines.launch
import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.Wallet
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

class WalletManager(socket: Socket,
                    sessionStore: SessionStore,
                    jsonAdapter: JsonAdapter,
                    dispatcherProvider: DispatcherProvider,
                    logger: Logger = EmptyLogger)
    : Wallet, WalletConnectCore(isDApp = false, socket, sessionStore, jsonAdapter, dispatcherProvider, logger) {

    private val LogTag = "Wallet"

    // region Session
    override fun approveSession(chainId: Int,
                                accounts: List<String>) {
        if (!initialized.get()) {
            logger.error(LogTag, "#approveSession(): !initialized")
            return
        }
        // dApp might think not approved, but wallet knows approved. dApp can send new sessionRequest, wallet should be able to accept again
        // if (sessionApproved.get()) return
        val sessionRequestId = sessionRequestId
        if (sessionRequestId == null) {
            logger.error(LogTag, "#approveSession(): There is no sessionRequest to accept!")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#approveSession(): There is no sessionRequest to accept!"))
            return
        }
        val currentSessionState = sessionState?.copy()
        if (currentSessionState == null) {
            // [sessionState] was updated on sessionRequest, it shouldn't be null
            logger.error(LogTag, "#approveSession(): sessionState is null")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#approveSession(): sessionState is null"))
            return
        }

        logger.info(LogTag, "#approveSession(chainId:$chainId, accounts:${accounts.joinToString()})")
        coroutineScope.launch {
            val model = SessionResponse(peerId = initialState.myPeerId,
                                        peerMeta = initialState.myPeerMeta,
                                        approved = true,
                                        chainId = chainId,
                                        accounts = accounts)
            val payload = JsonRpcResponse(id = sessionRequestId,
                                          result = model)
            val payloadAsJson = serializePayload(payload,
                                                 getResponseType(SessionResponse::class.java))
                                ?: return@launch

            val encryptedPayload = encryptPayload(payloadAsJson)
                                   ?: return@launch
            val message = SocketMessage(topic = currentSessionState.remotePeerId,
                                        type = SocketMessageType.Pub,
                                        payload = encryptedPayload)
            publish(message,
                    queueIfDisconnected = true)

            // update [sessionState] of Wallet. remotePeerId, remotePeerMeta was updated on sessionRequest
            sessionState = currentSessionState.copy(
                    chainId = chainId,
                    accounts = accounts,

                    updatedAt = System.currentTimeMillis()
            )
            sessionApproved.set(true)
            sessionStore.persist(initialState.connectionParams.topic,
                                 sessionState!!)

            // notify wallet
            callback(SessionCallback.SessionApproved(
                    messageId = sessionRequestId,
                    chainId = chainId,
                    accounts = accounts,
                    remotePeerId = currentSessionState.remotePeerId,
                    remotePeerMeta = currentSessionState.remotePeerMeta
            ))
        }

    }

    override fun rejectSession(errorMessage: String?) {
        if (!initialized.get()) {
            logger.error(LogTag, "#rejectSession(): !initialized")
            return
        }
        // dApp might think not approved, but wallet knows approved. dApp can send new sessionRequest, wallet should be able to reject this time
        /*if (sessionApproved.get()) {
            // use [updateSessionRequest]. sessionRequestId might not be available (after restoring from prev. session)
            return
        }*/
        val sessionRequestId = sessionRequestId
        if (sessionRequestId == null) {
            logger.error(LogTag, "#approveSession(): sessionRequestId inconsistency!")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#approveSession(): sessionRequestId inconsistency!"))
            return
        }

        logger.info(LogTag, "#rejectSession('$errorMessage')")
        val finalErrorMessage = errorMessage ?: "Session is Rejected"
        coroutineScope.launch {
            val payload = JsonRpcError(id = sessionRequestId,
                                       error = JsonRpcErrorData(RpcErrorCode.Server,
                                                                finalErrorMessage))
            val payloadAsJson = serializePayload(payload,
                                                 JsonRpcError::class.java)
                                ?: return@launch

            val encryptedPayload = encryptPayload(payloadAsJson)
                                   ?: return@launch

            val currentSessionState = sessionState?.copy()
            if (currentSessionState == null) {
                // [sessionState] was updated on sessionRequest, it shouldn't be null
                logger.error(LogTag, "#rejectSession(): sessionState is null")
                failureCallback(Failure(type = FailureType.SessionError,
                                        message = "#rejectSession(): sessionState is null"))
                return@launch
            }

            val message = SocketMessage(topic = currentSessionState.remotePeerId,
                                        type = SocketMessageType.Pub,
                                        payload = encryptedPayload)
            publish(message,
                    queueIfDisconnected = true)

            // update [sessionState] of Wallet. remotePeerId, remotePeerMeta was updated on sessionRequest
            sessionState = currentSessionState.copy(
                    chainId = null,
                    accounts = null,

                    updatedAt = System.currentTimeMillis()
            )
            sessionApproved.set(false)

            // notify wallet
            callback(SessionCallback.SessionRejected(finalErrorMessage))

            close(deleteLocal = true,
                  deleteRemote = false)
        }
    }

    override fun updateSession(chainId: Int?,
                               accounts: List<String>?,
                               approved: Boolean) {
        if (!initialized.get()) {
            logger.error(LogTag, "#updateSessionRequest(): !initialized")
            return
        }
        if (!sessionApproved.get()) {
            logger.warning(LogTag, "#updateSessionRequest(): session wasn't approved before, you can't update!")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#updateSessionRequest(): session wasn't approved before, you can't update!"))
            return
        }
        val currentSessionState = sessionState?.copy()
        if (currentSessionState == null) {
            // [sessionState] was updated on sessionRequest, it shouldn't be null
            logger.warning(LogTag, "#updateSessionRequest(): sessionState is null")
            return
        }

        logger.info(LogTag,
                    "#updateSessionRequest(approved:$approved, chainId:$chainId, accounts:${accounts?.joinToString()})")

        coroutineScope.launch {
            val model = SessionUpdate(approved = approved,
                                      chainId = chainId,
                                      accounts = accounts)
            val messageId = generateMessageId()
            val payload = JsonRpcRequest(id = messageId,
                                         method = SessionRpcMethod.Update,
                                         params = listOf(model))

            val payloadAsJson = serializePayload(payload,
                                                 getRequestType(SessionUpdate::class.java))
                                ?: return@launch

            val encryptedPayload = encryptPayload(payloadAsJson)
                                   ?: return@launch

            val message = SocketMessage(topic = currentSessionState.remotePeerId,
                                        type = SocketMessageType.Pub,
                                        payload = encryptedPayload)
            publish(message,
                    queueIfDisconnected = true)

            // update [sessionState] of Wallet. remotePeerId, remotePeerMeta was updated on sessionRequest
            sessionState = currentSessionState.copy(
                    chainId = chainId,
                    accounts = accounts,

                    updatedAt = System.currentTimeMillis()
            )
            sessionApproved.set(approved)

            if (approved) {
                sessionStore.persist(initialState.connectionParams.topic,
                                     sessionState!!)

                // notify wallet
                callback(SessionCallback.SessionUpdated(
                        messageId = messageId,
                        chainId = chainId,
                        accounts = accounts
                ))
            } else {
                callback(SessionCallback.SessionDeleted(byMe = true))
                close(deleteLocal = true,
                      deleteRemote = false)
            }
        }
    }
    // endregion

    // region Custom Request/Response
    override fun approveRequest(messageId: Long,
                                result: Any?,
                                resultType: Type) {
        if (!initialized.get()) {
            logger.error(LogTag, "#approveRequest(): !initialized")
            return
        }
        if (!sessionApproved.get()) {
            logger.warning(LogTag, "#approveRequest(): session isn't approved")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#approveRequest(): session isn't approved"))
            return
        }

        coroutineScope.launch {
            val payload = JsonRpcResponse(id = messageId,
                                          result = result)

            val finalMessageId: Long? = encryptPayloadAndPublish(messageId,
                                                                 payload,
                                                                 getResponseType(resultType),
                                                                 queueIfDisconnected = true)

            // notify wallet
            if (finalMessageId != null) {
                val method = messageMethodMap[messageId]
                logger.info(LogTag, "approveRequest($messageId), $method")

                when (method) {
                    is CustomRpcMethod -> {
                        callback(RequestCallback.CustomResponse(messageId,
                                                                result))
                    }
                    null -> {
                        // messageMethodMap[messageId] data is somehow lost (activity/process death?)
                        callback(RequestCallback.CustomResponse(messageId,
                                                                result))
                    }

                    is EthRpcMethod -> {
                        when (method) {
                            EthRpcMethod.Sign -> {
                                callback(RequestCallback.EthSignResponse(messageId,
                                                                         result as String))
                            }
                            EthRpcMethod.PersonalSign -> {
                                callback(RequestCallback.EthSignResponse(messageId,
                                                                         result as String))
                            }
                            EthRpcMethod.SignTypedData -> {
                                callback(RequestCallback.EthSignResponse(messageId,
                                                                         result as String))
                            }

                            EthRpcMethod.SignTransaction -> {
                                callback(RequestCallback.EthSignTxResponse(messageId,
                                                                           result as String))
                            }
                            EthRpcMethod.SendRawTransaction -> {
                                callback(RequestCallback.EthSendRawTxResponse(messageId,
                                                                              result as String?))
                            }
                            EthRpcMethod.SendTransaction -> {
                                callback(RequestCallback.EthSendTxResponse(messageId,
                                                                           result as String?))
                            }
                        }
                    }

                    else -> {
                        logger.error(LogTag, "approveRequest() has unexpected method:$method. " +
                                             "Must be one of [CustomRpcMethod, EthRpcMethod]")
                        failureCallback(Failure(type = FailureType.InvalidResponse,
                                                message = "approveRequest() has unexpected method:$method. " +
                                                          "Must be one of [CustomRpcMethod, EthRpcMethod]"))
                    }
                }
            }
        }
    }

    override fun rejectRequest(messageId: Long,
                               errorType: JsonRpcErrorData?) {
        if (!initialized.get()) {
            logger.error(LogTag, "#rejectRequest(): !initialized")
            return
        }
        if (!sessionApproved.get()) {
            logger.warning(LogTag, "#rejectRequest(): session isn't approved")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#approveRequest(): session isn't approved"))
            return
        }

        coroutineScope.launch {
            val payload = JsonRpcError(id = messageId,
                                       error = errorType ?: JsonRpcErrorData(RpcErrorCode.Server,
                                                                             "Request is Rejected!"))
            val finalMessageId: Long? = encryptPayloadAndPublish(messageId,
                                                                 payload,
                                                                 JsonRpcError::class.java,
                                                                 queueIfDisconnected = true)

            // notify wallet
            if (finalMessageId != null) {
                val method = messageMethodMap[messageId]
                logger.info(LogTag, "rejectRequest($messageId), $method")
                callback(RequestCallback.RequestRejected(messageId,
                                                         payload.error))
            }
        }
    }
    // endregion

    // region Incoming Messages
    override suspend fun handleJsonRpcRequest(decryptedPayload: String,
                                              method: String?,
                                              messageId: Long) {
        logger.debug(LogTag, "#handleJsonRpcRequest(method:$method, id:$messageId, decryptedPayload:$decryptedPayload)")

        try {
            when (method) {
                null -> {
                    logger.error(LogTag, "#handleJsonRpcRequest(): 'method' is not String")
                    failureCallback(Failure(type = FailureType.DeserializeSessionMessage))
                    reportInvalidSocketMessage(messageId,
                                               JsonRpcErrorData(RpcErrorCode.RequestMethodNotAvailable,
                                                                "'method' is not String"))
                    return
                }

                SessionRpcMethod.Request.value -> {
                    logger.debug(LogTag, "Wallet received SessionRequest")
                    val payload: JsonRpcRequest<SessionRequest> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(SessionRequest::class.java)
                    ) ?: return

                    // update for wallet to respond back to dApp
                    sessionRequestId = messageId

                    // update [sessionState] of Wallet
                    this.sessionState = SessionState(
                            connectionParams = initialState.connectionParams,

                            myPeerId = initialState.myPeerId,
                            myPeerMeta = initialState.myPeerMeta,
                            remotePeerId = payload.params[0].peerId,
                            remotePeerMeta = payload.params[0].peerMeta,

                            // do not update yet, we didn't agree on anything yet
                            chainId = null,
                            accounts = null,

                            updatedAt = System.currentTimeMillis()
                    )
                    // we don't persist yet

                    // notify wallet
                    callback(SessionCallback.SessionRequested(
                            messageId,
                            payload.params[0].chainId,
                            payload.params[0].peerId,
                            payload.params[0].peerMeta
                    ))
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
                        callback(SessionCallback.SessionDeleted(byMe = false))
                        close(deleteLocal = true,
                              deleteRemote = false)
                        return
                    }
                }

                EthRpcMethod.Sign.value -> {
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "EthSign but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "EthSign but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<String> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(String::class.java)
                    ) ?: return

                    if (payload.params.size < 2) {
                        logger.warning(LogTag, "EthSign but params.size < 2")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "EthSign but params.size < 2"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSign but params.size < 2"))
                        return
                    }
                    if (!isAddressApproved(payload.params[0])) {
                        rejectRequest(messageId, JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                  "Address is not approved!"))
                        return
                    }

                    val data = EthSign(address = payload.params[0],
                                       message = payload.params[1],
                                       type = SignType.Sign)
                    try {
                        data.validate()
                    } catch (error: Exception) {
                        logger.warning(LogTag, "EthSign: ${error.stackTraceToString()}")
                        failureCallback(error.toFailure(type = FailureType.InvalidRequest,
                                                        message = "EthSign: ${error.stackTraceToString()}"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSign content is not valid!"))
                        return
                    }

                    // save for wallet
                    messageMethodMap[messageId] = EthRpcMethod.Sign
                    // notify wallet
                    callback(RequestCallback.EthSignRequested(messageId, data))
                }
                EthRpcMethod.PersonalSign.value -> {
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "EthPersonalSign but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "EthPersonalSign but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<String> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(String::class.java)
                    ) ?: return

                    if (payload.params.size < 2) {
                        logger.warning(LogTag, "EthPersonalSign but params.size < 2")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "EthPersonalSign but params.size < 2"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthPersonalSign but params.size < 2"))
                        return
                    }
                    if (!isAddressApproved(payload.params[1])) {
                        rejectRequest(messageId, JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                  "Address is not approved!"))
                        return
                    }

                    val data = EthSign(address = payload.params[1],
                                       message = payload.params[0],
                                       type = SignType.PersonalSign)
                    try {
                        data.validate()
                    } catch (error: Exception) {
                        logger.warning(LogTag, "EthPersonalSign: ${error.stackTraceToString()}")
                        failureCallback(error.toFailure(type = FailureType.InvalidRequest,
                                                        message = "EthPersonalSign: ${error.stackTraceToString()}"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthPersonalSign content is not valid!"))
                        return
                    }

                    // save for wallet
                    messageMethodMap[messageId] = EthRpcMethod.PersonalSign
                    // notify wallet
                    callback(RequestCallback.EthSignRequested(messageId, data))

                }
                EthRpcMethod.SignTypedData.value -> {
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "EthSignTyped but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "EthSignTyped but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<Any> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(Any::class.java)
                    ) ?: return

                    if (payload.params.size < 2) {
                        logger.warning(LogTag, "EthSignTyped but params.size < 2")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "EthSignTyped but params.size < 2"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSignTyped but params.size < 2"))
                        return
                    }
                    if (!isAddressApproved(payload.params[0] as String)) {
                        rejectRequest(messageId, JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                  "Address is not approved!"))
                        return
                    }

                    // save for wallet
                    messageMethodMap[messageId] = EthRpcMethod.SignTypedData
                    // notify wallet
                    callback(RequestCallback.EthSignTypedDataRequested(messageId,
                                                                       payload.params[0] as String,
                                                                       payload.params[1]))
                }

                EthRpcMethod.SignTransaction.value -> {
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "EthSignTransaction but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "EthSignTransaction but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<EthTransaction> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(EthTransaction::class.java)
                    ) ?: return

                    if (payload.params.isEmpty()) {
                        logger.warning(LogTag, "EthSignTransaction but params.size < 1")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "EthSignTransaction but params.size < 1"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSignTransaction but params.size < 2"))
                        return
                    }

                    try {
                        payload.params[0].validate()
                    } catch (error: Exception) {
                        logger.warning(LogTag, "EthSignTransaction: ${error.stackTraceToString()}")
                        failureCallback(error.toFailure(type = FailureType.InvalidRequest,
                                                        message = "EthSignTransaction: ${error.stackTraceToString()}"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSignTransaction content is not valid!"))
                        return
                    }

                    if (!isAddressApproved(payload.params[0].from)) {
                        rejectRequest(messageId, JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                  "Address is not approved!"))
                        return
                    }

                    // save for wallet
                    messageMethodMap[messageId] = EthRpcMethod.SignTransaction
                    // notify wallet
                    callback(RequestCallback.EthSignTxRequested(messageId,
                                                                payload.params[0]))
                }
                EthRpcMethod.SendRawTransaction.value -> {
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "EthSendRawTx but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "EthSendRawTx but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<String> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(String::class.java)
                    ) ?: return

                    if (payload.params.isEmpty()) {
                        logger.warning(LogTag, "EthSendRawTx but params.size < 1")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "EthSendRawTx but params.size < 1"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSendRawTx but params.size < 2"))
                        return
                    }

                    // save for wallet
                    messageMethodMap[messageId] = EthRpcMethod.SendRawTransaction
                    // notify wallet
                    callback(RequestCallback.EthSendRawTxRequested(messageId,
                                                                   payload.params[0]))
                }
                EthRpcMethod.SendTransaction.value -> {
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "EthSendTx but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "EthSendTx but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<EthTransaction> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(EthTransaction::class.java)
                    ) ?: return

                    if (payload.params.isEmpty()) {
                        logger.warning(LogTag, "EthSendTx but params.size < 1")
                        failureCallback(Failure(type = FailureType.InvalidRequest,
                                                message = "EthSendTx but params.size < 1"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSendTx but params.size < 2"))
                        return
                    }

                    try {
                        payload.params[0].validate()
                    } catch (error: Exception) {
                        logger.warning(LogTag, "EthSendTx: ${error.stackTraceToString()}")
                        failureCallback(error.toFailure(type = FailureType.InvalidRequest,
                                                        message = "EthSendTx: ${error.stackTraceToString()}"))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                    "EthSendTx content is not valid!"))
                        return
                    }

                    if (!isAddressApproved(payload.params[0].from)) {
                        rejectRequest(messageId, JsonRpcErrorData(RpcErrorCode.InvalidRequestParams,
                                                                  "Address is not approved!"))
                        return
                    }

                    // save for wallet
                    messageMethodMap[messageId] = EthRpcMethod.SendTransaction
                    // notify wallet
                    callback(RequestCallback.EthSendTxRequested(messageId,
                                                                payload.params[0]))
                }

                else -> {
                    // custom
                    if (!sessionApproved.get()) {
                        logger.warning(LogTag, "CustomRequest but !sessionApproved")
                        failureCallback(Failure(type = FailureType.SessionError,
                                                message = "CustomRequest but !sessionApproved"))
                        return
                    }
                    val payload: JsonRpcRequest<Any> = deserializePayload(
                            decryptedPayload,
                            messageId,
                            getRequestType(Any::class.java)
                    ) ?: return

                    // save for wallet
                    messageMethodMap[messageId] = CustomRpcMethod(method)
                    // notify wallet
                    callback(RequestCallback.CustomRequested(messageId,
                                                             method,
                                                             payload.params))
                }
            }
        } catch (error: Exception) {
            // List<Any>[0] might throw
            logger.error(LogTag, "#handleJsonRpcRequest(): ${error.stackTraceToString()}")
            failureCallback(error.toFailure(type = FailureType.DeserializeSessionMessage))
            reportInvalidSocketMessage(messageId,
                                       JsonRpcErrorData(RpcErrorCode.InvalidJson,
                                                        "Error while trying to process the message content!"))
        }
    }

    private fun isAddressApproved(address: String)
            : Boolean {
        return sessionState?.accounts?.contains(address) ?: false
    }

    override suspend fun handleJsonRpcResponse(decryptedPayload: String,
                                               messageId: Long) {
        logger.debug(LogTag, "#handleJsonRpcResponse(id:$messageId, payload:$decryptedPayload)")

        // NoOp
    }

    override suspend fun handleJsonRpcError(decryptedPayload: String,
                                            messageId: Long) {
        logger.debug(LogTag, "#handleJsonRpcError(id:$messageId, payload:$decryptedPayload)")
        val payload: JsonRpcError = deserializePayload(
                decryptedPayload,
                messageId,
                JsonRpcError::class.java
        ) ?: return

        /** only messages from [reportInvalidSocketMessage] */
        failureCallback(Failure(type = FailureType.SessionError,
                                message = payload.error.toString()))
    }

    override suspend fun handleCustom(decryptedPayload: String,
                                      messageId: Long) {
        logger.debug(LogTag, "#handleCustom(id:$messageId, payload:$decryptedPayload)")

        callback(RequestCallback.CustomResponse(messageId,
                                                decryptedPayload))

    }
    // endregion

}
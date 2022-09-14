/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session

import walletconnect.core.session.callback.CallbackData
import walletconnect.core.session.callback.SessionCallback
import walletconnect.core.session.model.InitialSessionState
import walletconnect.core.session_state.SessionStore
import walletconnect.core.socket.Socket

typealias Fresh = Boolean

/**
 * - Session lifecycle is between [openSocketAsync] and [closeAsync] calls.
 *   After [closeAsync], this instance can be reused again with new [openSocketAsync] call.
 * - You can use [disconnectSocketAsync], [reconnectSocketAsync] if needed, they do NOT reset state.
 *   If disconnected & reconnected, it automatically subscribes to all messages (myPeerId, topic) internally again
 * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized with lock internally.
 */
interface SessionLifecycle {

    /**
     * Start of lifecycle. Opens [Socket] and subscribes to all messages (myPeerId, topic) **asynchronously**
     * - Idempotent
     * - Thread safe
     * - Restores previous Session if exists (by topic)
     * - Instance is reusable after [closeAsync]
     * - If there is fatal error on [Socket], [closeAsync] is called internally
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     * - Callback might be called from different Threads, be aware when doing UI rendering in it.
     *
     * @param[onOpened] notified when openSocket is finalized. It will always be called even if it was already open,
     *                Fresh will be false on that case
     */
    fun openSocketAsync(initialState: InitialSessionState,
                        callback: ((CallbackData) -> Unit)?,
                        onOpened: ((Fresh) -> Unit)?)

    /**
     * Start of lifecycle. Opens [Socket] and subscribes to all messages (myPeerId, topic)
     * - Idempotent
     * - Thread safe
     * - Restores previous Session if exists (by topic)
     * - Instance is reusable after [closeAsync]
     * - If there is fatal error on [Socket], [closeAsync] is called internally
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     * - Callback might be called from different Threads, be aware when doing UI rendering in it.
     */
    suspend fun openSocket(initialState: InitialSessionState,
                           callback: ((CallbackData) -> Unit)?)
            : Fresh

    /** Returns [InitialSessionState] if instance is initialized, otherwise null */
    fun getInitialSessionState()
            : InitialSessionState?

    /**
     * - Disconnects socket connection **asynchronously**. States are NOT reset.
     * - Idempotent
     * - Thread Safe
     * - If disconnected & reconnected, you are not subscribed to topic, myPeerId anymore,
     *   you need to resubscribe after reconnected. This is done automatically.
     * - If there are messages in incoming queue or outgoing queue of [Socket], they are not deleted
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     */
    fun disconnectSocketAsync(onRequested: (() -> Unit)? = null)

    /**
     * - Disconnects socket connection. States are NOT reset.
     * - Idempotent
     * - Thread Safe
     * - If disconnected & reconnected, you are not subscribed to topic, myPeerId anymore,
     *   you need to resubscribe after reconnected. This is done automatically.
     * - If there are messages in incoming queue or outgoing queue of [Socket], they are not deleted
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     */
    suspend fun disconnectSocket()

    /**
     * - Reconnect socket connection **asynchronously**.
     * - Idempotent
     * - Thread Safe
     * - If disconnected & reconnected, you are not subscribed to topic, myPeerId anymore,
     *   you need to resubscribe after reconnected. This is done automatically.
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     */
    fun reconnectSocketAsync(onRequested: (() -> Unit)? = null)

    /**
     * - Reconnect socket connection.
     * - Idempotent
     * - Thread Safe
     * - If disconnected & reconnected, you are not subscribed to topic, myPeerId anymore,
     *   you need to resubscribe after reconnected. This is done automatically.
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     */
    suspend fun reconnectSocket()

    /**
     * End of lifecycle. Closes [Socket], resets states **asynchronously**.
     * - Returns immediately, but has internal delay to send last message to remotePeer.
     *   To get notified when completed, use [onClosed] (so you know when you can call [openSocketAsync] again)
     * - All calls after closed are ignored (can't trigger callback, because it is released)
     * - Sends `updateSession(approved=false)` message to remotePeer if sessionApproved
     * - Idempotent.
     * - Thread Safe
     * - Caller is expected to broadcast [SessionCallback], except when [deleteLocal] and [deleteRemote] are false.
     *   Because [deleteLocal] and [deleteRemote] are not enough to decide on other cases
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     * - You can call [openSocketAsync] again, instance is reusable.
     *
     * @param[deleteLocal] If true, session data is removed from local [SessionStore]
     * @param[deleteRemote] If true, peer is notified
     * @param[delayMs] delays the closing of [Socket], so last published messages can be sent to peer.
     *                 Min delay of 500ms is forced internally
     * @param[onClosed] notified when close is finalized. It will always be called even if it was already close,
     *                 Fresh will be false on that case
     */
    fun closeAsync(deleteLocal: Boolean,
                   deleteRemote: Boolean,
                   delayMs: Long = 1_250L,
                   onClosed: ((Fresh) -> Unit)? = null)

    /**
     * End of lifecycle. Closes [Socket], resets states
     * - All calls after closed are ignored (can't trigger callback, because it is released)
     * - Sends `updateSession(approved=false)` message to remotePeer if sessionApproved
     * - Idempotent.
     * - Thread Safe
     * - Caller is expected to broadcast [SessionCallback], except when [deleteLocal] and [deleteRemote] are false.
     *   Because [deleteLocal] and [deleteRemote] are not enough to decide on other cases
     * - [openSocketAsync], [disconnectSocketAsync], [reconnectSocketAsync], [closeAsync] method calls are synchronized.
     * - You can call [openSocketAsync] again, instance is reusable.
     *
     * @param[deleteLocal] If true, session data is removed from local [SessionStore]
     * @param[deleteRemote] If true, peer is notified
     * @param[delayMs] delays the closing of [Socket], so last published messages can be sent to peer.
     *                 Min delay of 500ms is forced internally
     */
    suspend fun close(deleteLocal: Boolean,
                      deleteRemote: Boolean,
                      delayMs: Long = 1_250L)
            : Fresh

}
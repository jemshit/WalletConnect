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

/**
 * - Session lifecycle is between [openSocket] and [close] calls.
 *   After [close], this instance can be reused again with new [openSocket] call.
 * - You can use [disconnectSocket], [reconnectSocket] if needed, they do NOT reset state.
 *   If disconnected & reconnected, it automatically subscribes to all messages (myPeerId, topic) internally again
 */
interface SessionLifecycle {

    /**
     * Start of lifecycle. Opens [Socket] and subscribes to all messages (myPeerId, topic)
     * - Idempotent
     * - Thread safe
     * - Restores previous Session if exists (by topic)
     * - Instance is reusable after [close]
     * - If there is fatal error on [Socket], [close] is called internally
     * - [openSocket], [disconnectSocket], [reconnectSocket], [close] method calls are synchronized.
     * - This is blocking call, may not be appropriate for UI threads.
     * - Callback might be called from different Threads, be aware when doing UI rendering in it.
     */
    fun openSocket(initialState: InitialSessionState,
                   callback: ((CallbackData) -> Unit)?)

    /** Returns [InitialSessionState] if instance is initialized, otherwise null */
    fun getInitialSessionState()
            : InitialSessionState?

    /**
     * - Disconnects socket connection. States are NOT reset.
     * - Idempotent
     * - Thread Safe
     * - If disconnected & reconnected, you are not subscribed to topic, myPeerId anymore,
     *   you need to resubscribe after reconnected. This is done automatically.
     * - If there are messages in incoming queue or outgoing queue of [Socket], they are not deleted
     * - [openSocket], [disconnectSocket], [reconnectSocket], [close] method calls are synchronized.
     */
    fun disconnectSocket()

    /**
     * - Idempotent
     * - Thread Safe
     * - If disconnected & reconnected, you are not subscribed to topic, myPeerId anymore,
     *   you need to resubscribe after reconnected. This is done automatically.
     * - [openSocket], [disconnectSocket], [reconnectSocket], [close] method calls are synchronized.
     */
    fun reconnectSocket()

    /**
     * End of lifecycle. Closes [Socket], reset states.
     * - Returns immediately, but has internal delay to send last message to remotePeer.
     *   To get notified when completed, use [onClosed] (so you know when you can call [openSocket] again)
     * - All calls after closed are ignored (can't report, because callback is released)
     * - Idempotent.
     * - Thread Safe
     * - Caller is expected to broadcast [SessionCallback], except when [deleteLocal] and [deleteRemote] are false.
     *   Because [deleteLocal] and [deleteRemote] are not enough to decide on other cases
     * - [openSocket], [disconnectSocket], [reconnectSocket], [close] method calls are synchronized.
     * - You can call [openSocket] again, instance is reusable.
     *
     * @param[deleteLocal] If true, session data is removed from local [SessionStore]
     * @param[deleteRemote] If true, peer is notified
     * @param[delayMs] delays the closing of [Socket], so last published messages can be sent to peer.
     *                 Min delay of 500ms is forced internally
     * @param[onClosed] notified when closing is finalized (because there is async [delayMs])
     */
    fun close(deleteLocal: Boolean,
              deleteRemote: Boolean,
              delayMs: Long = 1_250L,
              onClosed: (() -> Unit)? = null)

}
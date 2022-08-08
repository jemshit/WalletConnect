/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.socket

import walletconnect.core.Failure
import walletconnect.core.socket.model.SocketConnectionState
import walletconnect.core.socket.model.SocketMessage

/**
 * - Socket lifecycle is between [open] and [close] calls.
 *   After [close], this instance can be reused again with new [open] call.
 * - If Socket is disconnected for some reason (by server, internet connection), retries multiple times internally.
 *   After some time if connection is not successful, [disconnect] is called internally.
 * - If you don't want retries, call [disconnect] externally
 * - Socket does not know about payload models, encryption/decryption, key etc...
 * - [open], [reconnect], [disconnect], [close] method calls are synchronized.
 * - If in [close]d state, [reconnect], [disconnect], [subscribeToAll], [unsubscribeFromAll], [publish] calls are ignored
 */
interface Socket {

    /**
     * Start of lifecycle.
     * - Idempotent
     * - Thread Safe
     * - Instance is reusable after [close]
     * - [open], [reconnect], [disconnect], [close] method calls are synchronized.
     *
     * @param[connectionListener] if same state is emitted consecutively, this is called only once
     * @param[errorListener] is called for any error on any method call.
     *                       Boolean indicates isFatal, if so, [close] is called internally.
     * @param[messageListener] if specified, [subscribeToAll] is called internally
     */
    fun open(connectionListener: (SocketConnectionState) -> Unit,
             errorListener: (Failure, Boolean) -> Unit,
             messageListener: ((SocketMessage) -> Unit)? = null)

    fun isConnected()
            : Boolean

    /**
     * - Disconnects socket connection. States are NOT reset.
     * - When connection is retried, after some time if it can't connect,
     *   it gives up and [disconnect] is called internally.
     * - Idempotent
     * - Thread Safe
     * - [open], [reconnect], [disconnect], [close] method calls are synchronized.
     */
    fun disconnect()

    /**
     * - Idempotent
     * - Thread Safe
     * - After some time if connection is not successful, [disconnect] is called internally.
     * - [open], [reconnect], [disconnect], [close] method calls are synchronized.
     */
    fun reconnect()

    /**
     * End of lifecycle. Reset all states.
     * - Unsubscribes all listeners ([SocketConnectionState], [SocketMessage], [Failure]).
     * - All calls after [Socket] is [close]d are ignored (can't report, because error callback is released)
     * - Idempotent.
     * - Thread Safe
     * - [open], [reconnect], [disconnect], [close] method calls are synchronized.
     * - You can call [open] again, instance is reusable.
     */
    fun close()

    /**
     * - Subscribes to all messages, only single [listener] is stored.
     * - Can be called right after [open] call, no need to wait for [isConnected]
     *
     * @return [SocketMessage], [SocketMessage.payload] is encrypted Json
     */
    fun subscribeToAll(listener: (SocketMessage) -> Unit)

    /**
     * - Unsubscribes [SocketMessage] listener
     * - Can be called right after [open] call, no need to wait for [isConnected]
     *
     * @return [SocketMessage], [SocketMessage.payload] is encrypted Json
     */
    fun unsubscribeFromAll()

    fun publish(message: SocketMessage,
                queueIfDisconnected: Boolean)

}
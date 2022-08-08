/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.socket.model

/**
 * - Communications are all relayed using WebSocket using JSON payloads with the following structure.
 * - Bridge Server acts as pub/sub controller which guarantees published messages are always received by their subscribers.
 *
 * @param[topic] Hex string, can contain '-'. It is either topic or peerId
 * @param[type] [SocketMessageType]
 * @param[payload] Data in JSON format
 */
data class SocketMessage(val topic: String,
                         val type: SocketMessageType,
                         val payload: String) {
    override fun toString(): String {
        return "SocketMessage(topic=$topic, type=$type)"
    }
}
/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.socket.model

/**
 * - Bridge Server acts as pub/sub controller which guarantees published messages are
 *   always received by their subscribers.
 * - Additionally the Bridge Server will trigger any existing push notifications' subscriptions that listen to
 *   any incoming payloads with matching topics.
 * - Don't forget to add custom serializer/deserializer for this Enum. Values should be "pub", "sub" in lowercase
 */
enum class SocketMessageType {
    Pub,
    Sub
}
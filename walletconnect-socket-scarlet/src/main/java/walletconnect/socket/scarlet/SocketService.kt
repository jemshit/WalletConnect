/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.socket.scarlet

import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow

/**
 * - Methods return different Channel instances per call!
 * - ReceiveChannel does not share emitted items across collectors, only one gets each event.
 *   BroadcastChannel does
 */
interface SocketService {

    @Receive
    fun observeConnectionEvents()
            : Flow<State>

    @Receive
    fun observeMessages()
            : Flow<WebSocket.Event>

    /** Sending when connection is stopped/destroyed does nothing */
    @Send
    fun <T> sendMessage(message: T)

}
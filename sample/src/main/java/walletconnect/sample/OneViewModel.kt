/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import walletconnect.core.requests.eth.EthSign
import walletconnect.core.requests.eth.SignType
import walletconnect.core.session_state.model.SessionState
import walletconnect.sample.store.SessionEvent
import java.util.*

class OneViewModel : ViewModel() {

    // dApp and wallet Fragments share same topic
    val topic: String by lazy { UUID.randomUUID().toString() }
    // "https://bridge.walletconnect.org" -> when one peer deletes session while other peer is disconnected, other peer never gets that message even after connecting
    // "https://safe-walletconnect.gnosis.io" -> if dApp subscribes to topic, messages sent to topic returns back to itself
    val bridgeUrl: String = "https://safe-walletconnect.gnosis.io"
    val symmetricKey: String = "3F4428472B4B6150645367566B5970337336763979244226452948404D635165"

    val sessionEvents: MutableSharedFlow<SessionEvent> = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val signRequests: MutableSharedFlow<EthSign> = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val signResponses: MutableSharedFlow<Pair<Long, String?>> = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun openSocket(tag: String,
                   sessionState: SessionState) {
        viewModelScope.launch {
            sessionEvents.tryEmit(SessionEvent.Open(tag, sessionState))
        }
    }

    fun signRequest(address: String,
                    message: String,
                    type: SignType) {
        signRequests.tryEmit(EthSign(address, message, type))
    }

    fun approveSignRequest(messageId: Long,
                           signature: String) {
        signResponses.tryEmit(Pair(messageId, signature))
    }

    fun rejectSignRequest(messageId: Long) {
        signResponses.tryEmit(Pair(messageId, null))
    }

    fun delete(tag: String,
               sessionState: SessionState) {
        viewModelScope.launch {
            sessionEvents.tryEmit(SessionEvent.Delete(tag, sessionState))
        }
    }

}
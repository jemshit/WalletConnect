/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

import walletconnect.core.session.model.InitialSessionState
import walletconnect.core.session.model.SessionRequest
import walletconnect.core.session.model.SessionResponse
import walletconnect.core.session.model.SessionUpdate

/**
 * - Consists fields of [SessionRequest], [SessionResponse], [SessionUpdate] models
 * - Instance should exist only if Session is approved
 */
data class SessionState(val connectionParams: ConnectionParams,

                        val myPeerId: String,
                        val myPeerMeta: PeerMeta,
                        val remotePeerId: String,
                        val remotePeerMeta: PeerMeta,

                        val chainId: Int?,
                        val accounts: List<String>?,

                        val updatedAt: Long)

fun SessionState.toInitialSessionState()
        : InitialSessionState {
    return InitialSessionState(
            connectionParams,
            myPeerId,
            myPeerMeta
    )
}
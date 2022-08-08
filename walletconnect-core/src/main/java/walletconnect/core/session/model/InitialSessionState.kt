/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model

import walletconnect.core.session_state.model.ConnectionParams
import walletconnect.core.session_state.model.PeerMeta

/** Used to initialize session connection */
data class InitialSessionState(val connectionParams: ConnectionParams,
                               val myPeerId: String,
                               val myPeerMeta: PeerMeta)
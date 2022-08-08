/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model

import walletconnect.core.session.model.json_rpc.SessionRpcMethod
import walletconnect.core.session_state.model.PeerMeta

/**
 * Request model of [SessionRpcMethod.Request].
 * The first dispatched JSON RPC request is the connection request including the details of the requesting peer
 *
 * @param[peerId] DApp's id
 * @param[peerMeta] DApp's metadata. In browser applications, this is scraped from the loaded webpage's
 *                  head meta-tags. In native applications, this is provided by the application developer.
 * @param[chainId] DApp requests accounts for this chainId
 */
data class SessionRequest(val peerId: String,
                          val peerMeta: PeerMeta,
                          val chainId: Int?)
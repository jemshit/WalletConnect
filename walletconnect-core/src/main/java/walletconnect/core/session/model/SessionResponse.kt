/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model

import walletconnect.core.session.model.json_rpc.SessionRpcMethod
import walletconnect.core.session_state.model.PeerMeta

/**
 * Response model of [SessionRpcMethod.Request]
 *
 * @param[peerId] Wallet peerId
 * @param[peerMeta] Wallet peerMeta. In browser applications, this is scraped from the loaded webpage's
 *                  head meta-tags. In native applications, this is provided by the application developer.
 * @param[approved] If session request is approved by Wallet.
 *                  If not approved, most wallets use [JsonRpcError] instead of this model
 * @param[chainId] Active chainId
 * @param[accounts] List of accounts for [chainId].
 */
data class SessionResponse(val peerId: String,
                           val peerMeta: PeerMeta,
                           val approved: Boolean,
                           val chainId: Int,
                           val accounts: List<String>?)
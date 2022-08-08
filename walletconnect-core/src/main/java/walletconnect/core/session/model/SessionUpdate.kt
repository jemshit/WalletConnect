/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model

import walletconnect.core.session.model.json_rpc.SessionRpcMethod

/**
 * Model of [SessionRpcMethod.Update]. Used when
 * - session is killed by DApp or Wallet
 * - wallet provides new accounts
 * - wallet changes the active chainId, accounts
 *
 * @param[approved] If not approved, session is deleted by both peers.
 * @param[chainId] Active chainId
 * @param[accounts] List of accounts for [chainId].
 */
data class SessionUpdate(val approved: Boolean,
                         val chainId: Int?,
                         val accounts: List<String>?)
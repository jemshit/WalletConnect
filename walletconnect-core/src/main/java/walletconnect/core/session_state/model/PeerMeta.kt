/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

data class PeerMeta(val name: String,
                    val url: String,
                    val description: String? = null,
                    val icons: List<String>? = null)
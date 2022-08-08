/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.cryptography.isHex
import walletconnect.core.toFailure
import walletconnect.core.util.isValidURL
import walletconnect.core.util.splitQuery
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The initiator, is the first peer who requests connection (DApp). DApp posts an encrypted payload consisting of
 * one-time topic (used for handshake only) and connection request details to the Bridge Server.
 *
 * Then using the WalletConnect Standard URI format ([EIP-1328](https://eips.ethereum.org/EIPS/eip-1328)) DApp assembles
 * together the required parameters to establish the connection: (handshake) topic, bridge (url) and (symmetric) key.
 * Check [Spec]([Spec](https://docs.walletconnect.com/tech-spec#requesting-connection))
 *
 * ```
 * wc:{topic...}@{version...}?bridge={url...}&key={key...}
 * ```
 *
 * Other query string parameters are all optional. Example URL:
 *
 * ```
 * wc:8a5e5bdc-a0e4-4702-ba63-8f1a5655744f@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=41791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7a
 * ```
 *
 * @param[topic] one-time topic (used for handshake only). Reused when session is stored.
 * @param[version] Number (eg. 1.9.0).
 * @param[bridgeUrl] Bridge URL. Plain text, not encoded, it gets URL encoded internally.
 * @param[symmetricKey] Symmetric key hex string.
 */
data class ConnectionParams(val topic: String,
                            val version: String,
                            val bridgeUrl: String,
                            val symmetricKey: String) {

    /** @throws [Failure] if any parameter is not valid */
    fun toUri()
            : String {

        validateTopic(topic)
        validateVersion(version)
        validateSymmetricKey(symmetricKey)
        validateBridgeUrl(bridgeUrl)

        val bridgeEncoded = try {
            URLEncoder.encode(bridgeUrl, "UTF-8")!! // Charsets.UTF_8 leads to crash on Android o.O
        } catch (error: Exception) {
            throw Failure(type = FailureType.InvalidBridgeUrl,
                          message = error.message,
                          cause = error)
        }

        return "wc:${topic}@${version}?bridge=${bridgeEncoded}&key=${symmetricKey}"
    }

    companion object {

        /** @throws [Failure] if [uriCandidate] is not valid */
        fun fromUri(uriCandidate: String)
                : ConnectionParams {

            if (uriCandidate.isBlank() || !uriCandidate.startsWith("wc:")) {
                throw Failure(type = FailureType.InvalidUri)
            }

            val uriString = uriCandidate
                    .replace("wc:", "wc://")
                    // 'java.net.URL' doesn't recognize wc:// protocol
                    .replace("wc://", "https://")
            val uri = try {
                URL(uriString)
            } catch (error: Exception) {
                throw Failure(type = FailureType.InvalidUri, cause = error)
            }
            val parameters: Map<String, List<String>> = try {
                uri.splitQuery().mapValues { entry ->
                    entry.value.filterNotNull()
                }
            } catch (error: Exception) {
                throw error.toFailure(type = FailureType.InvalidUri)
            }

            val topic = validateTopic(uri.userInfo)
            val version = validateVersion(uri.host)
            val bridgeUrlEncoded = try {
                parameters["bridge"]!!.firstOrNull()
            } catch (error: Exception) {
                throw Failure(type = FailureType.InvalidBridgeUrl,
                              message = error.message,
                              cause = error)
            }
            validateBridgeUrl(bridgeUrlEncoded)

            val bridgeUrlPlain = try {
                URLDecoder.decode(bridgeUrlEncoded, Charsets.UTF_8)!!
            } catch (error: Exception) {
                throw Failure(type = FailureType.InvalidBridgeUrl,
                              message = error.message,
                              cause = error)
            }
            validateBridgeUrl(bridgeUrlPlain)

            val symmetricKey = try {
                parameters["key"]!!.firstOrNull()
            } catch (error: Exception) {
                throw Failure(type = FailureType.InvalidSymmetricKey,
                              message = error.message,
                              cause = error)
            }
            validateSymmetricKey(symmetricKey)

            return ConnectionParams(topic,
                                    version,
                                    bridgeUrlPlain,
                                    symmetricKey!!)
        }

        /** @throws [Failure] */
        private fun validateTopic(topic: String?)
                : String {
            if (topic.isNullOrBlank()) {
                throw Failure(type = FailureType.InvalidTopic)
            }
            return topic
        }

        /** @throws [Failure] */
        private fun validateVersion(version: String?)
                : String {
            if (version.isNullOrBlank()) {
                throw Failure(type = FailureType.InvalidVersion)
            }
            return version
        }

        /** @throws [Failure] */
        private fun validateSymmetricKey(symmetricKey: String?)
                : String {
            if (symmetricKey.isNullOrBlank()) {
                throw Failure(type = FailureType.InvalidSymmetricKey)
            }
            if (!symmetricKey.isHex()) {
                throw Failure(type = FailureType.InvalidSymmetricKey)
            }
            return symmetricKey
        }

        /** @throws [Failure] */
        private fun validateBridgeUrl(bridgeUrl: String?)
                : String {
            if (bridgeUrl.isNullOrBlank()) {
                throw Failure(type = FailureType.InvalidBridgeUrl)
            }
            if (!bridgeUrl.isValidURL()) {
                throw Failure(type = FailureType.InvalidBridgeUrl)
            }
            return bridgeUrl
        }

    }

}
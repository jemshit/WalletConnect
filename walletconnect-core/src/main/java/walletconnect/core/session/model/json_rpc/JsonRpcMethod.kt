/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model.json_rpc

sealed interface JsonRpcMethod {
    val value: String
}

sealed class SessionRpcMethod(override val value: String)
    : JsonRpcMethod {

    /**
     * The first dispatched JSON RPC request is the connection request including
     * the details of the requesting peer
     */
    object Request
        : SessionRpcMethod("wc_sessionRequest")

    /**
     * Used when
     * - session is killed by DApp or Wallet
     * - wallet provides new accounts
     * - wallet changes the active chainId, accounts
     */
    object Update
        : SessionRpcMethod("wc_sessionUpdate")

    override fun toString(): String {
        return this.value
    }

}

sealed class EthRpcMethod(override val value: String)
    : JsonRpcMethod {

    /**
     * The sign method calculates an Ethereum specific signature with:
     * ```sign(keccak256("\x19Ethereum Signed Message:\n" + len(message) + message)))```
     *
     * - By adding a prefix to the message makes the calculated signature recognisable as an Ethereum specific signature.
     *   This prevents misuse where a malicious DApp can sign arbitrary data (e.g. transaction) and use the signature
     *   to impersonate the victim.
     * - Data is Raw data, not hex string! Check [1](https://github.com/WalletConnect/walletconnect-docs/issues/32),
     *   [2](https://github.com/WalletConnect/walletconnect-monorepo/blob/7573fa9e1d91588d4af3409159b4fd2f9448a0e2/packages/clients/core/src/index.ts#L646),
     *   [3](https://github.com/WalletConnect/WalletConnectSwift/blob/e0bbfd16d5c54a7aa7e316956d765399c6332fa2/Sources/PublicInterface/Client.swift#L100)
     * - Metamask returns `eth_sign requires 32 byte message hash`.
     * - Note: See `ecRecover` to verify the signature.
     * - Response: Signature in hex string
     */
    object Sign
        : EthRpcMethod("eth_sign")

    /**
     * The sign method calculates an Ethereum specific signature with:
     * ```sign(keccak256("\x19Ethereum Signed Message:\n" + len(message) + message)))```
     *
     * - By adding a prefix to the message makes the calculated signature recognisable as an Ethereum specific signature.
     *   This prevents misuse where a malicious DApp can sign arbitrary data (e.g. transaction) and use the signature
     *   to impersonate the victim.
     * - Data is hex string. Check [1](https://github.com/WalletConnect/walletconnect-docs/issues/32),
     *   [2](https://github.com/WalletConnect/walletconnect-monorepo/blob/7573fa9e1d91588d4af3409159b4fd2f9448a0e2/packages/clients/core/src/index.ts#L660),
     *   [3](https://github.com/WalletConnect/WalletConnectSwift/blob/e0bbfd16d5c54a7aa7e316956d765399c6332fa2/Sources/PublicInterface/Client.swift#L82)
     * - Note: See `ecRecover` to verify the signature.
     * - Response: Signature in hex string
     */
    object PersonalSign
        : EthRpcMethod("personal_sign")

    /**
     * The sign method calculates an Ethereum specific signature with:
     * ```sign(keccak256("\x19Ethereum Signed Message:\n" + len(message) + message)))```
     *
     * By adding a prefix to the message makes the calculated signature recognisable as an Ethereum specific signature.
     * This prevents misuse where a malicious DApp can sign arbitrary data (e.g. transaction) and use the signature
     * to impersonate the victim.
     *
     * Note: See `ecRecover` to verify the signature.
     *
     * Response: Signature in hex string
     */
    object SignTypedData
        : EthRpcMethod("eth_signTypedData")

    /**
     * Signs a transaction that can be submitted to the network at a later time using with `eth_sendRawTransaction`
     *
     * Response: signed transaction data, hex string
     */
    object SignTransaction
        : EthRpcMethod("eth_signTransaction")

    /**
     * Creates new message call transaction or a contract creation for signed transactions.
     *
     * Response: 32 Bytes - the transaction hash, or the zero hash if the transaction is not yet available.
     *           Use `eth_getTransactionReceipt` to get the contract address, after the transaction was mined,
     *           when you created a contract.
     */
    object SendRawTransaction
        : EthRpcMethod("eth_sendRawTransaction")

    /**
     * Creates new message call transaction or a contract creation, if the data field contains code.
     *
     * It is a combination of `eth_signTransaction` and `eth_sendRawTransaction`.
     *
     * Response: 32 Bytes - the transaction hash, or the zero hash if the transaction is not yet available.
     *           Use `eth_getTransactionReceipt` to get the contract address, after the transaction was mined,
     *           when you created a contract.
     */
    object SendTransaction
        : EthRpcMethod("eth_sendTransaction")

    override fun toString(): String {
        return this.value
    }

}

class CustomRpcMethod(override val value: String)
    : JsonRpcMethod {

    override fun toString(): String {
        return this.value
    }

}

package walletconnect.requests

import walletconnect.core.session.model.json_rpc.CustomRpcMethod

object CustomRpcMethods {

    /**
     * [EIP-3326](https://ethereum-magicians.org/t/eip-3326-wallet-switchethereumchain/5471).
     *
     * If the error code (error.code) is 4902, then the requested chain has not been added by MetaMask,
     * and you have to request to add it via `wallet_addEthereumChain`
     *
     * Response: The method returns null if the request was successful, and an error otherwise.
     */
    val SwitchEthChain: CustomRpcMethod
        get() = CustomRpcMethod("wallet_switchEthereumChain")

}
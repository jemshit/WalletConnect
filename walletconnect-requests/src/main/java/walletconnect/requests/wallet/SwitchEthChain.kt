package walletconnect.requests.wallet

import walletconnect.requests.CustomRpcMethods

/**
 * Model for [CustomRpcMethods.SwitchEthChain]
 *
 * [EIP-3326](https://ethereum-magicians.org/t/eip-3326-wallet-switchethereumchain/5471)
 *
 * @param[chainId] A 0x-prefixed hexadecimal string
 */
data class SwitchChain(val chainId: String)
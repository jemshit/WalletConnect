/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.requests.eth

import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.cryptography.isHex
import walletconnect.core.session.model.json_rpc.EthRpcMethod

/**
 * Model for [EthRpcMethod.SignTransaction], [EthRpcMethod.SendTransaction]
 *
 * @param[from] address the transaction is send from
 * @param[to] address the transaction is directed to. Optional when creating new contract.
 *            Contract creation occurs when there is no to value but there is a data value.
 *            When sending custom token, [to] is address of smart contract, and address of peer is embedded in [data]
 * @param[data] The compiled code of a contract OR the hash of the invoked method signature and
 *              encoded parameters. For details see [Ethereum Contract ABI.](https://docs.soliditylang.org/en/develop/abi-spec.html)
 *              Contract creation occurs when there is no to value but there is a data value.
 *              This can be empty.
 * @param[chainId] In Hex. [Check](https://chainlist.org/)
 * @param[gas] In Hex. Integer of the gas provided for the transaction execution. It will return unused gas.
 *             (optional, default: 90000)
 * @param[gasPrice] In Hex. Integer of the gasPrice used for each paid gas. (optional, default: To-Be-Determined)
 * @param[gasLimit] In Hex. Gas limit is a highly optional parameter, and wallet automatically calculate a reasonable price for it.
 *                  You will probably know that your smart contract benefits from a custom gas limit if it ever does for some reason.
 * @param[maxFeePerGas] In Hex. introduced in eip-1559. [check](https://docs.alchemy.com/alchemy/guides/eip-1559/maxpriorityfeepergas-vs-maxfeepergas)
 * @param[maxPriorityFeePerGas] In Hex. introduced in eip-1559. [check](https://docs.alchemy.com/alchemy/guides/eip-1559/maxpriorityfeepergas-vs-maxfeepergas)
 * @param[value] In Hex. Integer of the value sent with this transaction (optional).
 *               When sending custom token, this should be is empty or 0x00. [value] is embedded in [data]
 * @param[nonce] In Hex. Integer of a nonce. This allows to overwrite your own pending transactions that use the same nonce. (optional)
 */
data class EthTransaction(val from: String,
                          val to: String?,
                          val data: String,
                          val chainId: String?,

                          val gas: String?,
                          val gasPrice: String?,
                          val gasLimit: String?,
                          val maxFeePerGas: String?,
                          val maxPriorityFeePerGas: String?,

                          val value: String?,
                          val nonce: String?) {

    /**
     * @throws [Failure] with [FailureType.InvalidHexString], [FailureType.InvalidEthAddress]
     */
    fun validate()
            : EthTransaction {
        // don't validate case sensitive address checksum, protocol doesn't enforce it

        // from
        if (!from.startsWith("0x")) {
            throw Failure(type = FailureType.InvalidHexString,
                          message = "'from' doesn't start with 0x")
        }
        // must be 20 Byte = 40 chars
        val fromHex = from.substring(2)
        if (!fromHex.isHex() || fromHex.length != 40) {
            throw Failure(type = FailureType.InvalidEthAddress,
                          message = "'from' invalid Hex or length (must be 40 chars)")
        }

        // to
        if (!to.isNullOrBlank()) {
            if (!to.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'to' doesn't start with 0x")
            }
            // must be 20 Byte = 40 chars
            val toHex = to.substring(2)
            if (!toHex.isHex() || toHex.length != 40) {
                throw Failure(type = FailureType.InvalidEthAddress,
                              message = "'to' invalid Hex or length (must be 40 chars)")
            }
        }
        if (to.isNullOrBlank() && data.isBlank()) {
            throw Failure(type = FailureType.InvalidEthAddress,
                          message = "Contract creation occurs when there is no to value but there is a data value. " +
                                    "Data is absent here")
        }

        // gas
        if (!gas.isNullOrBlank()) {
            if (!gas.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'gas' doesn't start with 0x")
            }
            val gasHex = gas.substring(2)
            if (!gasHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'gas' invalid Hex")
            }
        }

        // gasPrice
        if (!gasPrice.isNullOrBlank()) {
            if (!gasPrice.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'gasPrice' doesn't start with 0x")
            }
            val gasPriceHex = gasPrice.substring(2)
            if (!gasPriceHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'gasPrice' invalid Hex")
            }
        }

        // gasLimit
        if (!gasLimit.isNullOrBlank()) {
            if (!gasLimit.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'gasLimit' doesn't start with 0x")
            }
            val gasLimitHex = gasLimit.substring(2)
            if (!gasLimitHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'gasLimit' invalid Hex")
            }
        }

        // maxFeePerGas
        if (!maxFeePerGas.isNullOrBlank()) {
            if (!maxFeePerGas.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'maxFeePerGas' doesn't start with 0x")
            }
            val maxFeePerGasHex = maxFeePerGas.substring(2)
            if (!maxFeePerGasHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'maxFeePerGas' invalid Hex")
            }
        }

        // maxPriorityFeePerGas
        if (!maxPriorityFeePerGas.isNullOrBlank()) {
            if (!maxPriorityFeePerGas.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'maxFeePerGas' doesn't start with 0x")
            }
            val maxPriorityFeePerGasHex = maxPriorityFeePerGas.substring(2)
            if (!maxPriorityFeePerGasHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'maxPriorityFeePerGas' invalid Hex")
            }
        }

        // value
        if (!value.isNullOrBlank()) {
            if (!value.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'value' doesn't start with 0x")
            }
            val valueHex = value.substring(2)
            if (!valueHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'value' invalid Hex")
            }
        }

        // nonce
        if (!nonce.isNullOrBlank()) {
            if (!nonce.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'nonce' doesn't start with 0x")
            }
            val nonceHex = nonce.substring(2)
            if (!nonceHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'nonce' invalid Hex")
            }
        }

        return this
    }

}
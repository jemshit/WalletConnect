/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.requests.eth

object FakeEthSign {

    val ValidSign = EthSign(address = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                            message = "Raw String",
                            type = SignType.Sign)

    val ValidPersonalSign = EthSign(address = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                                    message = "0x52617720537472696e67", // Raw String
                                    type = SignType.PersonalSign)

    val InvalidAddress = EthSign(address = "0x02C4288c13cD24B9dC5dFb2bB637756e36a4bB2600",
                                 message = "Raw String",
                                 type = SignType.Sign)

    val InvalidPersonalSign = EthSign(address = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                                      message = "Raw String",
                                      type = SignType.PersonalSign)

}
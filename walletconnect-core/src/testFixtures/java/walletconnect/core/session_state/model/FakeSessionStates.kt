/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

/** Topics must be unique */
object FakeSessionStates {
    val Normal = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "100"
            ),

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal2,

            chainId = 1,
            accounts = listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                              "0x621261D26847B423Df639848Fb53530025a008e8"),

            updatedAt = System.currentTimeMillis()
    )
    val InvalidConnectionParams = SessionState(
            connectionParams = FakeConnectionParams.InvalidBridgeUrl,

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = 1,
            accounts = listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                              "0x621261D26847B423Df639848Fb53530025a008e8"),

            updatedAt = System.currentTimeMillis()
    )
    val EmptyClientPeerId = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "101"
            ),

            myPeerId = "",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = 1,
            accounts = listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                              "0x621261D26847B423Df639848Fb53530025a008e8"),

            updatedAt = System.currentTimeMillis()
    )
    val EmptyRemotePeerId = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "102"
            ),

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = 1,
            accounts = listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                              "0x621261D26847B423Df639848Fb53530025a008e8"),

            updatedAt = System.currentTimeMillis()
    )
    val NegativeChainId = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "103"
            ),

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = -1,
            accounts = listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                              "0x621261D26847B423Df639848Fb53530025a008e8"),

            updatedAt = System.currentTimeMillis()
    )
    val NullAccounts = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "105"
            ),

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = 1,
            accounts = null,

            updatedAt = System.currentTimeMillis()
    )
    val EmptyAccounts = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "106"
            ),

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = 1,
            accounts = listOf(),

            updatedAt = System.currentTimeMillis()
    )
    val ZeroUpdatedAt = SessionState(
            connectionParams = FakeConnectionParams.Normal.copy(
                    topic = FakeConnectionParams.Normal.topic + "107"
            ),

            myPeerId = "clientPeerId",
            myPeerMeta = FakePeerMeta.Normal,
            remotePeerId = "remotePeerId",
            remotePeerMeta = FakePeerMeta.Normal,

            chainId = 1,
            accounts = listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                              "0x621261D26847B423Df639848Fb53530025a008e8"),

            updatedAt = 0L
    )

    val AllFakes: List<SessionState> = listOf(
            Normal,
            InvalidConnectionParams,
            EmptyClientPeerId,
            EmptyRemotePeerId,
            NegativeChainId,
            NullAccounts,
            EmptyAccounts,
            ZeroUpdatedAt
    )
}
/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

/** Topics must be unique */
object FakeConnectionParams {

    val Normal = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            version = "1",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18"
    )

    val EmptyTopic = ConnectionParams(
            topic = "",
            version = "1",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18"
    )

    val VersionSemver = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658-2",
            version = "1.2.3",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18"
    )
    val EmptyVersion = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658-3",
            version = "",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18"
    )

    val InvalidBridgeUrl = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658-4",
            version = "1",
            bridgeUrl = "some",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18"
    )

    val EmptyBridgeUrl = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658-5",
            version = "1",
            bridgeUrl = "",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18"
    )

    val ShortSymmetricKey = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658-6",
            version = "1",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = "1"
    )
    val EmptySymmetricKey = ConnectionParams(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658-7",
            version = "1",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = ""
    )
    val NonHexSymmetricKey = ConnectionParams(
            topic = "GHIJKLM090324",
            version = "1",
            bridgeUrl = "https://bridge.walletconnect.org",
            symmetricKey = "4941e24abe9cce7822c17ebeadcd2f25a96b6e6904b9e4ec0942446ad5de8a18JKHDfodipfwj"
    )


}
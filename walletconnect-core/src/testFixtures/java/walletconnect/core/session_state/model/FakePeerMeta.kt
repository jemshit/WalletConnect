/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

object FakePeerMeta {
    val Normal = PeerMeta(
            name = "Name",
            url = "https://google.com",
            description = "Some Desc",
            icons = listOf("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")
    )
    val Normal2 = PeerMeta(
            name = "Name2",
            url = "https://google2.com",
            description = "Some Desc2",
            icons = listOf("https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/MetaMask_Fox.svg/1200px-MetaMask_Fox.svg.png")
    )
    val EmptyName = PeerMeta(
            name = "",
            url = "https://google.com",
            description = "Some Desc",
            icons = listOf("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")
    )

    val InvalidUrl = PeerMeta(
            name = "Name",
            url = "com",
            description = "Some Desc",
            icons = listOf("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")
    )
    val EmptyUrl = PeerMeta(
            name = "Name",
            url = "",
            description = "Some Desc",
            icons = listOf("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")
    )

    val NullDesc = PeerMeta(
            name = "Name",
            url = "https://google.com",
            description = null,
            icons = listOf("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")
    )
    val EmptyDesc = PeerMeta(
            name = "Name",
            url = "https://google.com",
            description = "",
            icons = listOf("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")
    )

    val NullIcons = PeerMeta(
            name = "Name",
            url = "https://google.com",
            description = "Some Desc",
            icons = null
    )
    val EmptyIcons = PeerMeta(
            name = "Name",
            url = "https://google.com",
            description = "Some Desc",
            icons = emptyList()
    )
}
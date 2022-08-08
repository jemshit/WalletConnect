/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.store

import walletconnect.core.session_state.model.SessionState

sealed class SessionEvent(val tag: String,
                          val sessionState: SessionState) {

    class Open(tag: String,
               sessionState: SessionState)
        : SessionEvent(tag, sessionState)

    class Delete(tag: String,
                 sessionState: SessionState)
        : SessionEvent(tag, sessionState)

}
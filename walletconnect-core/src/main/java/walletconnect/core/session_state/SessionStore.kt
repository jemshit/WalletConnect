/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state

import kotlinx.coroutines.flow.Flow
import walletconnect.core.session_state.model.SessionState

/**
 * Stores and manages <topic-[SessionState]> pair.
 *
 * Restores data stored asynchronously at initialization.
 * While restore is in progress, all methods suspend the caller.
 */
interface SessionStore {

    /**
     * Suspends if Session restore is in progress.
     * Otherwise returns [SessionState] from in-memory cache (Map).
     */
    suspend fun get(topic: String)
            : SessionState?

    /**
     * Suspends if Session restore is in progress.
     * Otherwise returns all [SessionState]s from in-memory cache (Map).
     *
     * @return If no data, either null or [emptyList] is returned
     */
    suspend fun getAll()
            : List<SessionState>?

    /**
     * Returns hot Flow of Set<[SessionState]>, replays last state.
     * - Set can be empty
     * - Only distinct items are emitted
     */
    fun getAllAsFlow()
            : Flow<Set<SessionState>>

    /**
     * Stores new <topic-[SessionState]> pair or updates existing entry.
     *
     * Write operations are synchronized between [persist], [remove], [removeAll].
     */
    suspend fun persist(topic: String,
                        state: SessionState)

    /**
     * Removes <topic-[SessionState]> pair if exists.
     *
     * Write operations are synchronized between [persist], [remove], [removeAll].
     */
    suspend fun remove(topic: String)

    /**
     * Removes all <topic-[SessionState]> pairs.
     *
     * Write operations are synchronized between [persist], [remove], [removeAll].
     */
    suspend fun removeAll()

}
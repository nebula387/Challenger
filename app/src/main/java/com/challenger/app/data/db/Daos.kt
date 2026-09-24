package com.challenger.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ChallengeDao {

    @Query("SELECT * FROM challenges WHERE archived = 0 ORDER BY sortOrder, createdAt")
    fun observeActive(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges ORDER BY archived, sortOrder, createdAt")
    fun observeAll(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE id = :id")
    fun observeById(id: Long): Flow<Challenge?>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getById(id: Long): Challenge?

    @Query("SELECT * FROM challenges WHERE archived = 0")
    suspend fun getActive(): List<Challenge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: Challenge): Long

    @Update
    suspend fun update(challenge: Challenge)

    @Delete
    suspend fun delete(challenge: Challenge)

    @Query("UPDATE challenges SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}

@Dao
interface CompletionDao {

    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE challengeId = :challengeId ORDER BY date DESC")
    fun observeByChallenge(challengeId: Long): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE challengeId = :challengeId ORDER BY date DESC")
    suspend fun getByChallenge(challengeId: Long): List<Completion>

    @Query("SELECT * FROM completions WHERE date = :date")
    suspend fun getByDate(date: LocalDate): List<Completion>

    @Query("SELECT * FROM completions WHERE challengeId = :challengeId AND date = :date LIMIT 1")
    suspend fun get(challengeId: Long, date: LocalDate): Completion?

    @Upsert
    suspend fun upsert(completion: Completion)

    @Query("DELETE FROM completions WHERE challengeId = :challengeId AND date = :date")
    suspend fun remove(challengeId: Long, date: LocalDate)

    @Query("DELETE FROM completions WHERE challengeId = :challengeId")
    suspend fun removeAllFor(challengeId: Long)
}

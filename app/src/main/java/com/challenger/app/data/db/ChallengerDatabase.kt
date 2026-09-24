package com.challenger.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion

@Database(
    entities = [Challenge::class, Completion::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChallengerDatabase : RoomDatabase() {

    abstract fun challengeDao(): ChallengeDao
    abstract fun completionDao(): CompletionDao

    companion object {
        @Volatile private var instance: ChallengerDatabase? = null

        fun get(context: Context): ChallengerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChallengerDatabase::class.java,
                    "challenger.db"
                ).build().also { instance = it }
            }
    }
}

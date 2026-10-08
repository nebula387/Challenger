package com.challenger.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import com.challenger.app.data.model.Freeze

@Database(
    entities = [Challenge::class, Completion::class, Freeze::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChallengerDatabase : RoomDatabase() {

    abstract fun challengeDao(): ChallengeDao
    abstract fun completionDao(): CompletionDao
    abstract fun freezeDao(): FreezeDao

    companion object {
        @Volatile private var instance: ChallengerDatabase? = null

        /**
         * Паузы появились во второй версии. Миграция обязана быть ручной:
         * уничтожать базу нельзя, в ней вся история челленджей.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `freezes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startDate` INTEGER NOT NULL, " +
                        "`endDate` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): ChallengerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChallengerDatabase::class.java,
                    "challenger.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}

package com.dermalens.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dermalens.app.data.model.User

@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class DermaDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: DermaDatabase? = null

        fun getDatabase(context: Context): DermaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DermaDatabase::class.java,
                    "dermalens_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.enigma.quiz_cash

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.enigma.quiz_cash.models.CategoryModel
import com.enigma.quiz_cash.models.QuestionModel

@Database(
    entities = [CategoryModel::class, QuestionModel::class],
    version = 1,
    exportSchema = false
)
abstract class DatabaseHelper : RoomDatabase() {
    abstract fun getDatabaseDao(): DatabaseDao

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null
        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                synchronized(this) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        DatabaseHelper::class.java,
                        "quiz_cash"
                    ).createFromAsset("quiz_cash.db")
                        .build()
                }
            }
            return instance!!
        }
    }
}
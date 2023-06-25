package com.enigma.quiz_cash

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.enigma.quiz_cash.models.CategoryModel
import com.enigma.quiz_cash.models.QuestionModel

@Dao
interface DatabaseDao {
    @Query("SELECT*FROM category")
    fun getAllCategory(): LiveData<List<CategoryModel>>

    @Query("SELECT*FROM questions WHERE topic LIKE :topic")
    fun getQuestions(topic: String): LiveData<List<QuestionModel>>
}
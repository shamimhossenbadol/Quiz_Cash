package com.enigma.quiz_cash.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "question")
    val question: String?,
    @ColumnInfo(name = "option1")
    val option_1: String?,
    @ColumnInfo(name = "option2")
    val option_2: String?,
    @ColumnInfo(name = "option3")
    val option_3: String?,
    @ColumnInfo(name = "option4")
    val option_4: String?,
    @ColumnInfo(name = "answer")
    val answer: String?,
    @ColumnInfo(name = "topic")
    val topic: String?
)

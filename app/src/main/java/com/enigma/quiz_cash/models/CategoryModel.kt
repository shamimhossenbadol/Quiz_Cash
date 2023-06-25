package com.enigma.quiz_cash.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "title")
    val title: String?,
    @ColumnInfo(name = "icon")
    val icon: String?,
    @ColumnInfo(name = "questions")
    val questions: Int
)
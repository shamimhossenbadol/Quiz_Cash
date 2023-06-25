package com.enigma.quiz_cash

import com.enigma.quiz_cash.models.CategoryModel

interface CategoryClicked {
    fun onClicked(category: CategoryModel, color: String)
}
package com.enigma.quiz_cash.models

data class User(
    var name: String? = null,
    var email: String? = null,
    var coins: Int? = null,
    var withdraw: Int? = null,
    var referCode: String? = null,
    var referCount: Int? = null,
    var referredBy: String? = null,
    var activeDate: String? = null
)
package com.attribution.sdk.data

data class AuthModel(
    val name: String,
    val password: String
)

data class AuthToken(
    val token: String,
    val message: String
)
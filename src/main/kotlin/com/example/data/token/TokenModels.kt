package com.example.data.token

import kotlinx.serialization.Serializable


@Serializable
data class RefreshRequest(
    val refreshToken: String
)


@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)
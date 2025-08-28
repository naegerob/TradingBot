package com.example.data.token

import com.example.data.ApiResponse
import kotlinx.serialization.Serializable


@Serializable
data class RefreshRequest(
    val refreshToken: String
): ApiResponse()

@Serializable
data class RefreshResponse(
    val accessToken: String
): ApiResponse()

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
): ApiResponse()

@Serializable
data class LoginResponse(
    val accessToken:    String,
    val refreshToken:   String
): ApiResponse()

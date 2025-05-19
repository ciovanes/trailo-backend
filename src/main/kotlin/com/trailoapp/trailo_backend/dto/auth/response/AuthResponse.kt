package com.trailoapp.trailo_backend.dto.auth.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.trailoapp.trailo_backend.dto.user.response.UserResponse

data class AuthResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("id_token")
    val idToken: String,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("expires_in")
    val expiresIn: Int,

    @JsonProperty("token_type")
    val tokenType: String,

    val user: UserResponse? = null
)
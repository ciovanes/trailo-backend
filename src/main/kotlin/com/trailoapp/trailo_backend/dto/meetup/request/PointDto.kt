package com.trailoapp.trailo_backend.dto.meetup.request

data class PointDto(
    val type: String,
    val coordinates: List<Double>
)
package com.trailoapp.trailo_backend.dto.meetup.request

import io.swagger.v3.oas.annotations.media.Schema

data class PointDto(
    @Schema(
        allowableValues = ["Point"],
    )
    val type: String,

    @Schema(
        description = "Coordinates as [longitude, latitude]",
        example = "[40.10942537564942, -3.6604914549817704]"
    )
    val coordinates: List<Double>
)
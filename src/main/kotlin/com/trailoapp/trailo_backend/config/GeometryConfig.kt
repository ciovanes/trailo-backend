package com.trailoapp.trailo_backend.config

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeometryConfig {

    @Bean
    fun geometryFactory(): GeometryFactory = GeometryFactory(PrecisionModel(), 4326)
}
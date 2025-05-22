package com.trailoapp.trailo_backend.config

import com.trailoapp.trailo_backend.repository.MeetupRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig (
    @Value("\${aws.cognito.userPoolId}") private val userPoolId: String,
    @Value("\${aws.region}") private val region: String
){
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        corsConfigurationSource: CorsConfigurationSource,
        customJwtAuthConverter: CustomJwtAuthenticationConverter, meetupRepository: MeetupRepository
    ): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { requests ->
                // auth endpoints
                requests.requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                // actuator endpoints
                requests.requestMatchers(HttpMethod.GET,"/actuator/health").permitAll()

                // users endpoints
                requests.requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                requests.requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").authenticated()
                requests.requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").authenticated()

                // friendships endpoints
                requests.requestMatchers(HttpMethod.GET, "/api/v1/friendships/**").authenticated()
                requests.requestMatchers(HttpMethod.POST, "/api/v1/friendships/**").authenticated()
                requests.requestMatchers(HttpMethod.PATCH, "/api/v1/friendships/**").authenticated()
                requests.requestMatchers(HttpMethod.DELETE, "/api/v1/friendships/**").authenticated()

                // groups requests
                requests.requestMatchers(HttpMethod.GET, "/api/v1/groups/**").authenticated()
                requests.requestMatchers(HttpMethod.POST, "/api/v1/groups/**").authenticated()
                requests.requestMatchers(HttpMethod.PATCH, "/api/v1/groups/**").authenticated()
                requests.requestMatchers(HttpMethod.DELETE, "/api/v1/groups/**").authenticated()

                // meetup
                requests.requestMatchers(HttpMethod.GET, "/api/v1/meetups/**").permitAll()
                requests.requestMatchers(HttpMethod.POST, "/api/v1/meetups/**").authenticated()
                requests.requestMatchers(HttpMethod.PATCH, "/api/v1/meetups/**").authenticated()
                requests.requestMatchers(HttpMethod.DELETE, "/api/v1/meetups/**").authenticated()

                // openAPI
                requests.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                    jwt.jwtAuthenticationConverter(customJwtAuthConverter)
                }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val issuerUri = "https://cognito-idp.$region.amazonaws.com/$userPoolId"
        val jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri) as NimbusJwtDecoder

        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuerUri)
        val tokenValidator = DelegatingOAuth2TokenValidator(withIssuer)

        jwtDecoder.setJwtValidator(tokenValidator)
        return jwtDecoder
    }
}
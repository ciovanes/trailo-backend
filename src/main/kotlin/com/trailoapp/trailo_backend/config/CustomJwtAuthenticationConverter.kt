package com.trailoapp.trailo_backend.config

import com.trailoapp.trailo_backend.service.UserService
import org.springframework.stereotype.Component
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter


@Component
class CustomJwtAuthenticationConverter(
    private val userService: UserService
): Converter<Jwt, AbstractAuthenticationToken> {
    private val defaultConverter = JwtAuthenticationConverter()

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val baseAuth = defaultConverter.convert(jwt)!!

        val sub = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(sub)
            ?: throw RuntimeException("User not found")

        return UsernamePasswordAuthenticationToken(user, jwt, baseAuth.authorities)
    }
}
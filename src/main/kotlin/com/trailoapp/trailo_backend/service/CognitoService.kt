package com.trailoapp.trailo_backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


@Service
class CognitoService(
    private val cognitoClient: CognitoIdentityProviderClient,
    private val userService: UserService
) {

    @Value("\${aws.cognito.userPoolId}")
    private lateinit var userPoolId: String

    @Value("\${aws.cognito.clientId}")
    private lateinit var clientId: String

    @Value("\${aws.cognito.clientSecret}")
    private lateinit var clientSecret: String

    /*
    * Register a new user with Cognito
    */
    fun registerUser(email: String, password: String, username: String, name: String?): String {
        // Create user attributes
        val userAttributes = mutableListOf<AttributeType>()

        userAttributes.add(AttributeType.builder()
            .name("email")
            .value(email)
            .build()
        )

        userAttributes.add(AttributeType.builder()
            .name("nickname")
            .value(username)
            .build()
        )

        name?.let {
            userAttributes.add(AttributeType.builder()
                .name("name")
                .value(it)
                .build())
        }

        // Calculate secret hash
        val secretHash = calculateSecretHash(username)

        // Create register request
        val signupRequest = SignUpRequest.builder()
            .clientId(clientId)
            .username(username)
            .password(password)
            .userAttributes(userAttributes)
            .secretHash(secretHash)
            .build()

        // Execute the request
        val response = cognitoClient.signUp(signupRequest)

        confirmUserSignUp(username)
        return response.userSub()
    }

    /*
    * Confirm the user's signup
    */
    fun confirmUserSignUp(username: String) {
        val request = AdminConfirmSignUpRequest.builder()
            .userPoolId(userPoolId)
            .username(username)
            .build()

        cognitoClient.adminConfirmSignUp(request)
    }

    /*
    * Log in a user with Cognito
    */
    fun loginUser(username: String, password: String): AuthenticationResultType {
        val authParams = buildMap {
            put("USERNAME", username)
            put("PASSWORD",  password)
            put("SECRET_HASH",  calculateSecretHash(username))
        }

        val request = AdminInitiateAuthRequest.builder()
            .userPoolId(userPoolId)
            .clientId(clientId)
            .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
            .authParameters(authParams)
            .build()

        val response = cognitoClient.adminInitiateAuth(request)
        return response.authenticationResult()
    }

    private fun calculateSecretHash(username: String): String {
        val message = username + clientId
        val signinKey = SecretKeySpec(clientSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(signinKey)
        val rawHmac = mac.doFinal(message.toByteArray(Charsets.UTF_8))

        return Base64.getEncoder().encodeToString(rawHmac)
    }
}
package com.trailoapp.trailo_backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient

@Configuration
class AwsConfig (
    @Value("\${aws.region}") private val awsRegion: String,
    @Value("\${aws.accessKey}") private val awsAccessKey: String,
    @Value("\${aws.secretKey}") private val awsSecretKey: String
){
    @Bean
    fun cognitoClient(): CognitoIdentityProviderClient {
        val credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
        )

        return CognitoIdentityProviderClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentials)
            .build()
    }
}
package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun createUser(email: String, username: String, name: String?, surname: String?, profileImageUrl: String?, country: String?, cognitoId: String): UserEntity {
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("Email already exists: $email")
        }

        if (userRepository.existsByUsername(username)) {
            throw IllegalArgumentException("Username already exists: $username")
        }

        val userEntity = UserEntity(
            email = email,
            username = username,
            name = name,
            surname = surname,
            profileImageUrl = profileImageUrl,
            country = country,
            cognitoId = cognitoId
        )

        return userRepository.save(userEntity)
    }

    fun findUserById(uuid: UUID): UserEntity? {
        return userRepository.findByIdOrNull(uuid)
    }

    fun findUserByEmail(email: String): UserEntity? {
        return  userRepository.findByEmail(email).orElse(null)
    }

    fun findUserByUsername(username: String): UserEntity? {
        return userRepository.findByUsername(username).orElse(null)
    }

    fun getAllUsers(): List<UserEntity> {
        return userRepository.findAll()
    }
}
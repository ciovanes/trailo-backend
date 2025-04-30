package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.user.request.UpdateUserRequest
import com.trailoapp.trailo_backend.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(private val userRepository: UserRepository, private val cognitoService: CognitoService) {

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

    @Transactional
    fun updateUser(uuid: UUID, updateRequest: UpdateUserRequest): UserEntity {
        val user = findUserById(uuid)
            ?: throw IllegalArgumentException("User does not exist: $uuid")

        updateRequest.name?.let { user.name = it }
        updateRequest.surname?.let { user.surname = it }
        updateRequest.profileImageUrl?.let { user.profileImageUrl = it }
        updateRequest.country?.let { user.country = it }

        return userRepository.save(user)
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

    fun findUserByCognitoId(cognitoId: String): UserEntity? {
        return userRepository.findByCognitoId(cognitoId).orElse(null)
    }

    fun getAllUsers(): List<UserEntity> {
        return userRepository.findAll()
    }

    fun getAllUsers(pageable: Pageable): Page<UserEntity> {
        return userRepository.findAll(pageable)
    }

    fun searchByUsername(query: String, pageable: Pageable): Page<UserEntity> {
        return userRepository.searchByUsernameContainingIgnoreCase(query, pageable)
    }

    fun searchByName(query: String, pageable: Pageable): Page<UserEntity> {
        return userRepository.searchByNameContainingIgnoreCase(query, pageable)
    }

    fun searchByCountry(query: String, pageable: Pageable): Page<UserEntity> {
        return userRepository.searchByCountryContainingIgnoreCase(query, pageable)
    }

    fun searchBySurname(query: String, pageable: Pageable): Page<UserEntity> {
        return userRepository.searchBySurnameContainingIgnoreCase(query, pageable)
    }

    @Transactional
    fun deleteUser(uuid: UUID, username: String) {
        cognitoService.deleteUser(username)
        userRepository.deleteById(uuid)
    }
}
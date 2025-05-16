package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.user.request.CreateUserDto
import com.trailoapp.trailo_backend.dto.user.request.UpdateUserRequest
import com.trailoapp.trailo_backend.exception.definitions.DuplicateResourceException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(private val userRepository: UserRepository, private val cognitoService: CognitoService) {

    // ===== BASIC USER OPERATIONS =====

    /**
     * Update the details of an existing user.
     *
     * @param uuid the UUID of the user to update.
     * @param updateRequest an [UpdateUserRequest] containing updated fields for the user.
     */
    @Transactional
    fun updateUser(uuid: UUID, updateRequest: UpdateUserRequest): UserEntity {
        val user = findUserById(uuid)
            ?: throw ResourceNotFoundException("user", uuid)

       updateRequest.apply {
           name?.let { user.name = it }
           surname?.let { user.surname = it }
           profileImageUrl?.let { user.profileImageUrl = it }
           country?.let { user.country = it }
       }

        return userRepository.save(user)
    }

    /**
     * Delete completely a user from the database and Cognito
     *
     * @param uuid the UUID of the user to delete.
     * @param username the username of the user to delete.
     */
    @Transactional
    fun deleteUser(uuid: UUID, username: String) {
        cognitoService.deleteUser(username)
        userRepository.deleteById(uuid)
    }

    // ===== SEARCH AND LISTING =====

    /**
     * Search for UserEntity based on the provided query and pagination information.
     *
     * @param query the search term to filter user entities.
     * @param pageable The [Pageable] object containing pagination and sorting information.
     * @return a page of [UserEntity] matching the search criteria.
     */
    fun searchQuery(searchBy: String, query: String, pageable: Pageable): Page<UserEntity> {
        return when(searchBy.lowercase()) {
            "username" -> searchByUsername(query, pageable)
            "name" -> searchByName(query, pageable)
            "surname" -> searchBySurname(query, pageable)
            "country" -> searchByCountry(query, pageable)
            else -> searchByUsername(query, pageable)
        }
    }

    fun getAllUsers(pageable: Pageable): Page<UserEntity> {
        return userRepository.findAll(pageable)
    }

    // ===== UTILITY METHODS =====

    @Transactional
    fun createUser(user: CreateUserDto): UserEntity {
        if (userRepository.existsByEmail(user.email)) {
            throw DuplicateResourceException("user", "email", user.email)
        }

        if (userRepository.existsByUsername(user.username)) {
            throw DuplicateResourceException("user", "username", user.username)
        }

        val userEntity = UserEntity(
            email = user.email,
            username = user.username,
            name = user.name,
            surname = user.surname,
            profileImageUrl = user.profileImageUrl,
            country = user.country,
            cognitoId = user.cognitoId
        )

        return userRepository.save(userEntity)
    }

    @Transactional
    fun saveUser(userEntity: UserEntity): UserEntity {
        return userRepository.save(userEntity)
    }

    fun findUserById(uuid: UUID): UserEntity? {
        return userRepository.findByIdOrNull(uuid)
    }

    fun findUserByEmail(email: String): UserEntity? {
        return  userRepository.findByEmail(email)
    }

    fun findUserByUsername(username: String): UserEntity? {
        return userRepository.findByUsername(username)
    }

    fun findUserByCognitoId(cognitoId: String): UserEntity? {
        return userRepository.findByCognitoId(cognitoId)
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
}
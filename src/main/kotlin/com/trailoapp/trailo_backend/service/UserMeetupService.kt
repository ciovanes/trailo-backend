package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.social.UserMeetupEntity
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.MeetupRepository
import com.trailoapp.trailo_backend.repository.UserMeetupRepository
import com.trailoapp.trailo_backend.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserMeetupService(
    private val userService: UserService,
    private val meetupService: MeetupService,
    private val groupService: GroupService,
    private val userMeetupRepository: UserMeetupRepository,
) {

    /**
     * Join a meetup.
     *
     * @param userId the UUID of the user who wants to join the meetup.
     * @param meetupId the UUID of the meetup to join.
     * @return the created [UserMeetupEntity].
     */
    @Transactional
    fun joinMeetup(userId: UUID, meetupId: UUID): UserMeetupEntity {
        val user = userService.findUserById(userId)
            ?: throw ResourceNotFoundException("user", userId)

        val meetup = meetupService.getMeetupOrThrow(meetupId)

        val group = groupService.getGroupByUuid(meetup.group)

        if (user.uuid == meetup.host) {
            throw BusinessRuleException("the host can't join their own meetup")
        }

        if (group.isPrivate) {
            groupService.getUserMembershipOrThrow(user.uuid, group.uuid)
        }

        return userMeetupRepository.save(
            UserMeetupEntity(
                user = user,
                meetup = meetup
            )
        )
    }

    /**
     * Leave a meetup.
     *
     * @param userId the UUID of the user who wants to leave the meetup.
     * @param meetupId the UUID of the meetup to leave.
     */
    @Transactional
    fun leaveMeetup(userId: UUID, meetupId: UUID) {
        val user = userService.findUserById(userId)
            ?: throw ResourceNotFoundException("user", userId)

        val meetup = meetupService.getMeetupOrThrow(meetupId)

        val userMeetup = meetupService.getUserMeetupOrThrow(user.uuid, meetup.uuid)

        userMeetupRepository.delete(userMeetup)
    }

}
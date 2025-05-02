package com.trailoapp.trailo_backend.config

import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class FriendshipStatusConverter : AttributeConverter<FriendshipStatus, String> {
    override fun convertToDatabaseColumn(attribute: FriendshipStatus?): String? {
        return attribute?.name
    }

    override fun convertToEntityAttribute(dbData: String?): FriendshipStatus? {
        return dbData?.let { FriendshipStatus.valueOf(it) }
    }
}
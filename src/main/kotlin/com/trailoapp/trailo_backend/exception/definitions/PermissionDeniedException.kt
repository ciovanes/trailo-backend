package com.trailoapp.trailo_backend.exception.definitions

class PermissionDeniedException (
    action: String,
    resourceType: String,
    resourceId: Any? = null
): AppException("Not allowed to $action $resourceType${if (resourceId != null) " '$resourceId'" else ""}")
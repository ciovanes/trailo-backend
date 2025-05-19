package com.trailoapp.trailo_backend.exception.definitions

class ResourceNotFoundException(
    resourceName: String,
    resourceId: Any? = null
): AppException("Resource '$resourceName' not found${if (resourceId != null) " with id '$resourceId'" else ""}")
package com.trailoapp.trailo_backend.exception.definitions

class DuplicateResourceException(
    resourceType: String,
    field: String,
    value: Any?
): AppException("A $resourceType with $field '$value' already exists")

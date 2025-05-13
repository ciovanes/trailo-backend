package com.trailoapp.trailo_backend.exception.definitions

class SelfActionException(action: String): AppException("You cannot $action yourself")

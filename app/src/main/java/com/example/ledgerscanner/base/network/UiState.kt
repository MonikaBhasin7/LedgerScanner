package com.example.ledgerscanner.base.network

sealed class UiState<T> {
    data class Success<T>(val data: T?) : UiState<T>()
    data class Loading<T>(val message: String? = null, val data: T? = null) : UiState<T>()
    data class Error<T>(val message: String, val throwable: Throwable? = null, val data: T? = null) :
        UiState<T>()
    class Idle<T> : UiState<T>()
}

sealed class OperationResult<T> {
    data class Success<T>(val data: T?) : OperationResult<T>()
    data class Error<T>(val message: String, val throwable: Throwable? = null) :
        OperationResult<T>()
}

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    object Success : OperationState()
    data class Error(val message: String) : OperationState()
}

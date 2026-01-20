package com.truerandom.util

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val status: Status
) {
    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    class Success<T>(data: T? = null) : Resource<T>(data, status = Status.SUCCESS)

    class Error<T>(data: T? = null, message: String? = null) : Resource<T>(data, message, status = Status.ERROR)

    class Loading<T>(data: T? = null) : Resource<T>(data, status = Status.LOADING)

    val isSuccess: Boolean get() = status  == Status.SUCCESS
    val isLoading: Boolean get() = status == Status.LOADING
    val isError: Boolean get() = status == Status.ERROR

    override fun toString(): String {
        return buildString {
            append("[${status.name}]")
            if (message != null) append(" Message: $message")
            if (data != null) append(" Data: $data")
        }
    }
}
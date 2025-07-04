package com.fusi24.pangreksa.web.security.domain

import java.io.Serializable
import java.util.*

class UserId private constructor(userId: String) : Serializable {
    val userId: String
    init {
        // TODO If the userId has a specific format, validate it here.
        this.userId = Objects.requireNonNull<String>(userId)
    }
    override fun toString(): String {
        return userId
    }
    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) return false
        val that = o as UserId
        return userId == that.userId
    }
    override fun hashCode(): Int {
        return Objects.hashCode(userId)
    }
    companion object {
        fun of(userId: String): UserId {
            return UserId(userId)
        }
    }
}

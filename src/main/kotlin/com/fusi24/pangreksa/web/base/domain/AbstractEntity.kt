package com.fusi24.pangreksa.web.base.domain

import jakarta.persistence.MappedSuperclass
import org.springframework.data.util.ProxyUtils

@MappedSuperclass
abstract class AbstractEntity<ID> {

    abstract fun getId(): ID?

    override fun toString(): String {
        return "${this::class.simpleName}{id=${getId()}}"
    }

    override fun hashCode(): Int {
        // Avoid using ID to compute hashCode for entity consistency.
        return ProxyUtils.getUserClass(this.javaClass).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true

        val thisUserClass = ProxyUtils.getUserClass(this.javaClass)
        val otherUserClass = ProxyUtils.getUserClass(other.javaClass)

        if (thisUserClass != otherUserClass) return false

        val id = getId()
        val otherId = (other as? AbstractEntity<*>)?.getId()

        return id != null && id == otherId
    }
}

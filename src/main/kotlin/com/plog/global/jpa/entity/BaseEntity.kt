package com.plog.global.jpa.entity

import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.Objects

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity protected constructor() {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long? = null
        protected set

    @CreatedDate
    var createDate: LocalDateTime? = null
        protected set

    @LastModifiedDate
    var modifyDate: LocalDateTime? = null
        protected set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BaseEntity
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }
}

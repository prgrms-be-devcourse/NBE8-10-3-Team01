package com.plog.global.jpa

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long? = null // 초기값 null (JPA가 할당)

    @CreatedDate
    @Column(updatable = false) // 생성일은 수정 불가 설정 추가 (권장)
    var createDate: LocalDateTime? = null

    @LastModifiedDate
    var modifyDate: LocalDateTime? = null

    // Kotlin은 getter/setter가 프로퍼티(var)에 자동 포함되므로 별도 작성 불필요

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // javaClass를 사용하여 정확한 클래스 비교 (자바의 getClass() != o.getClass() 대응)
        if (other == null || javaClass != other.javaClass) return false

        other as BaseEntity

        // id가 null이면 동등하지 않음 (영속화 전에는 다르다고 판단)
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        // id가 null이면 0 반환 (자바의 Objects.hashCode(id) 대응)
        return id?.hashCode() ?: 0
    }
}
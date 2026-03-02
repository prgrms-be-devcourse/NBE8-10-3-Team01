package com.plog.domain.image.entity

import com.plog.domain.member.entity.Member
import com.plog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * 게시글에 첨부되는 이미지 파일의 메타데이터를 관리하는 JPA 엔티티입니다.
 *
 * 물리적 파일은 Object Storage(MinIO)에 저장하고, DB에는 해당 파일의
 * 원본명, 저장된 키 값(Stored Name), 접근 URL 등 메타데이터만 보관합니다.
 *
 * **상속 정보:**
 * [BaseEntity]를 상속받아 생성일시(createdAt)와 수정일시(updatedAt)를 자동으로 관리합니다.
 *
 * **생명주기 관리:**
 * 스프링 빈이 아니며, JPA 영속성 컨텍스트(Persistence Context)에 의해 생명주기가 관리됩니다.
 *
 * **주요 패턴:**
 * `protected` 기본 생성자로 무분별한 기본 생성자 호출을 방지합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-20
 */
@Entity
class Image protected constructor() : BaseEntity() {

    enum class ImageDomain {
        POST, PROFILE
    }

    enum class ImageStatus {
        PENDING, USED
    }

    @Column(nullable = false)
    var originalName: String = ""

    @Column(nullable = false, unique = true)
    var storedName: String = ""

    @Column(nullable = false)
    var accessUrl: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var uploader: Member? = null

    @Column(nullable = false)  // nullable=false 로 변경!
    @Enumerated(EnumType.STRING)
    var domain: ImageDomain = ImageDomain.POST  // ← enum + 기본값!

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ImageStatus = ImageStatus.PENDING  // ← enum + 기본값!

    @Column
    var domainId: Long? = null     // 123L (Post ID 등)

    constructor(
        originalName: String,
        storedName: String,
        accessUrl: String,
        uploader: Member?,
        domain: ImageDomain = ImageDomain.POST,
        status: ImageStatus = ImageStatus.PENDING,
        domainId: Long? = null
    ) : this() {
        this.originalName = originalName
        this.storedName = storedName
        this.accessUrl = accessUrl
        this.uploader = uploader
        this.domain = domain
        this.status = status
        this.domainId = domainId
    }

    companion object {
        @JvmStatic
        fun builder() = ImageBuilder()
    }

    class ImageBuilder {

        private var originalName: String = ""
        private var storedName: String = ""
        private var accessUrl: String = ""
        private var uploader: Member? = null
        private var domain: ImageDomain = ImageDomain.POST
        private var status: ImageStatus = ImageStatus.PENDING
        private var domainId: Long? = null

        fun originalName(originalName: String) = apply { this.originalName = originalName }
        fun storedName(storedName: String) = apply { this.storedName = storedName }
        fun accessUrl(accessUrl: String) = apply { this.accessUrl = accessUrl }
        fun uploader(uploader: Member?) = apply { this.uploader = uploader }
        fun domain(domain: ImageDomain) = apply { this.domain = domain }
        fun status(status: ImageStatus) = apply { this.status = status }
        fun domainId(domainId: Long?) = apply { this.domainId = domainId }

        fun build() = Image(originalName, storedName, accessUrl, uploader,
            domain, status, domainId)
    }
}
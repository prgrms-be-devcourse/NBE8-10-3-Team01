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

    @Column(nullable = false)
    var originalName: String = ""

    @Column(nullable = false, unique = true)
    var storedName: String = ""

    @Column(nullable = false)
    var accessUrl: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var uploader: Member? = null

    constructor(
        originalName: String,
        storedName: String,
        accessUrl: String,
        uploader: Member?
    ) : this() {
        this.originalName = originalName
        this.storedName = storedName
        this.accessUrl = accessUrl
        this.uploader = uploader
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

        fun originalName(originalName: String) = apply { this.originalName = originalName }
        fun storedName(storedName: String) = apply { this.storedName = storedName }
        fun accessUrl(accessUrl: String) = apply { this.accessUrl = accessUrl }
        fun uploader(uploader: Member?) = apply { this.uploader = uploader }

        fun build() = Image(originalName, storedName, accessUrl, uploader)
    }
}
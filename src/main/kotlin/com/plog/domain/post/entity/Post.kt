package com.plog.domain.post.entity

import com.plog.domain.hashtag.entity.PostHashTag
import com.plog.domain.member.entity.Member
import com.plog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * 블로그 게시물의 핵심 데이터를 담당하는 엔티티 클래스입니다.
 * <p>
 * 마크다운 형식의 본문과 검색 최적화를 위한 순수 텍스트, 요약글 등을 관리하며,
 * JPA를 통해 MySQL의 MEDIUMTEXT 타입과 매핑되어 대용량 텍스트를 저장합니다.
 *
 * <p><b>상속 정보:</b><br>
 * [BaseEntity]를 상속받아 고유 식별자(id)와 생성/수정 시간을 공통으로 관리합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * JPA 프록시 생성을 위해 `protected` 수준의 기본 생성자가 포함되어 있습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Jakarta Persistence API를 사용합니다.
 *
 * @author MintyU
 * @since 2026-01-15
 * @see BaseEntity
 */
@Entity
class Post(
    @Column(nullable = false, length = 255)
    var title: String = "",

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    var content: String = "",

    @Column(length = 500)
    var summary: String? = null,

    @Enumerated(EnumType.STRING)
    var status: PostStatus = PostStatus.DRAFT,

    var viewCount: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var postHashTags: MutableList<PostHashTag> = mutableListOf(),

    @Column(length = 255)
    var thumbnail: String? = null
) : BaseEntity() {

    fun incrementViewCount() {
        this.viewCount++
    }

    fun update(title: String, content: String, summary: String?, thumbnail: String?) {
        this.title = title
        this.content = content
        this.summary = summary
        this.thumbnail = thumbnail
    }

    companion object {
        @JvmStatic
        fun builder() = PostBuilder()
    }

    // Keep builder for Java compatibility or if specifically requested, 
    // but Kotlin code should use the constructor.
    class PostBuilder {
        private var title: String = ""
        private var content: String = ""
        private var summary: String? = null
        private var status: PostStatus = PostStatus.DRAFT
        private var viewCount: Int = 0
        private var member: Member? = null
        private var postHashTags: MutableList<PostHashTag> = mutableListOf()
        private var thumbnail: String? = null

        fun title(title: String) = apply { this.title = title }
        fun content(content: String) = apply { this.content = content }
        fun summary(summary: String?) = apply { this.summary = summary }
        fun status(status: PostStatus) = apply { this.status = status }
        fun viewCount(viewCount: Int) = apply { this.viewCount = viewCount }
        fun member(member: Member?) = apply { this.member = member }
        fun postHashTags(postHashTags: MutableList<PostHashTag>) = apply { this.postHashTags = postHashTags }
        fun thumbnail(thumbnail: String?) = apply { this.thumbnail = thumbnail }

        fun build() = Post(
            title = title,
            content = content,
            summary = summary,
            status = status,
            viewCount = viewCount,
            member = member!!,
            postHashTags = postHashTags,
            thumbnail = thumbnail
        )
    }
}

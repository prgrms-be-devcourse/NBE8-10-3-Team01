package com.plog.domain.post.entity

import com.plog.domain.hashtag.entity.PostHashTag
import com.plog.domain.member.entity.Member
import com.plog.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.util.ArrayList

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
class Post : BaseEntity {
    @Column(nullable = false, length = 255)
    var title: String = ""
        protected set

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    var content: String = ""
        protected set

    @Column(length = 500)
    var summary: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    var status: PostStatus = PostStatus.DRAFT
        protected set

    var viewCount: Int = 0
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    lateinit var member: Member
        protected set

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var postHashTags: MutableList<PostHashTag> = ArrayList()
        protected set

    var thumbnail: String? = null
        protected set

    protected constructor() : super()

    constructor(
        title: String,
        content: String,
        summary: String?,
        status: PostStatus,
        viewCount: Int,
        member: Member,
        postHashTags: MutableList<PostHashTag>,
        thumbnail: String?
    ) : super() {
        this.title = title
        this.content = content
        this.summary = summary
        this.status = status
        this.viewCount = viewCount
        this.member = member
        this.postHashTags = postHashTags
        this.thumbnail = thumbnail
    }

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
        fun builder(): PostBuilder {
            return PostBuilder()
        }
    }

    class PostBuilder {
        private var title: String? = null
        private var content: String? = null
        private var summary: String? = null
        private var status: PostStatus = PostStatus.DRAFT
        private var viewCount: Int = 0
        private var member: Member? = null
        private var postHashTags: MutableList<PostHashTag> = ArrayList()
        private var thumbnail: String? = null

        fun title(title: String?): PostBuilder {
            this.title = title
            return this
        }

        fun content(content: String?): PostBuilder {
            this.content = content
            return this
        }

        fun summary(summary: String?): PostBuilder {
            this.summary = summary
            return this
        }

        fun status(status: PostStatus): PostBuilder {
            this.status = status
            return this
        }

        fun viewCount(viewCount: Int): PostBuilder {
            this.viewCount = viewCount
            return this
        }

        fun member(member: Member?): PostBuilder {
            this.member = member
            return this
        }

        fun postHashTags(postHashTags: MutableList<PostHashTag>): PostBuilder {
            this.postHashTags = postHashTags
            return this
        }

        fun thumbnail(thumbnail: String?): PostBuilder {
            this.thumbnail = thumbnail
            return this
        }

        fun build(): Post {
            return Post(
                title ?: "",
                content ?: "",
                summary,
                status,
                viewCount,
                member!!,
                postHashTags,
                thumbnail
            )
        }
    }
}

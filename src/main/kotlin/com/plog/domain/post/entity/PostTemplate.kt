package com.plog.domain.post.entity

import com.plog.domain.member.entity.Member
import com.plog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * 게시글 작성 시 게시글의 템플릿을 기존에 설정해 둘 수 있습니다. 이를 위한 엔티티 클래스입니다.
 * <p>
 * 제목, 본문, author 에 대한 데이터를 가지고 있습니다.
 *
 * <p><b>상속 정보:</b><br>
 * [BaseEntity]를 상속받아 고유 식별자(id)와 생성/수정 시간을 공통으로 관리합니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
@Entity
class PostTemplate : BaseEntity {

    @Column(nullable = false)
    var name: String = ""
        protected set

    @Column(nullable = false)
    var title: String = ""
        protected set

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    var content: String = ""
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    lateinit var member: Member
        protected set

    protected constructor() : super()

    constructor(name: String, title: String, content: String, member: Member) : super() {
        this.name = name
        this.title = title
        this.content = content
        this.member = member
    }

    companion object {
        @JvmStatic
        fun builder(): PostTemplateBuilder {
            return PostTemplateBuilder()
        }
    }

    class PostTemplateBuilder {
        private var name: String? = null
        private var title: String? = null
        private var content: String? = null
        private var member: Member? = null

        fun name(name: String?): PostTemplateBuilder {
            this.name = name
            return this
        }

        fun title(title: String?): PostTemplateBuilder {
            this.title = title
            return this
        }

        fun content(content: String?): PostTemplateBuilder {
            this.content = content
            return this
        }

        fun member(member: Member?): PostTemplateBuilder {
            this.member = member
            return this
        }

        fun build(): PostTemplate {
            return PostTemplate(name ?: "", title ?: "", content ?: "", member!!)
        }
    }

    fun update(name: String, title: String, content: String): PostTemplate {
        this.title = title
        this.content = content
        this.name = name
        return this
    }
}

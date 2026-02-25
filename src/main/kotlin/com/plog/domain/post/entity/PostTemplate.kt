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
class PostTemplate(
    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var title: String = "",

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    var content: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member
) : BaseEntity() {

    fun update(name: String, title: String, content: String): PostTemplate {
        this.name = name
        this.title = title
        this.content = content
        return this
    }

    companion object {
        @JvmStatic
        fun builder() = PostTemplateBuilder()
    }

    class PostTemplateBuilder {
        private var name: String = ""
        private var title: String = ""
        private var content: String = ""
        private var member: Member? = null

        fun name(name: String) = apply { this.name = name }
        fun title(title: String) = apply { this.title = title }
        fun content(content: String) = apply { this.content = content }
        fun member(member: Member?) = apply { this.member = member }

        fun build() = PostTemplate(
            name = name,
            title = title,
            content = content,
            member = member!!
        )
    }
}

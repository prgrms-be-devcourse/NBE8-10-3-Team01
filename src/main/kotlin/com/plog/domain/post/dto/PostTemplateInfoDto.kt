package com.plog.domain.post.dto

import com.plog.domain.post.entity.PostTemplate
import jakarta.validation.constraints.NotBlank

/**
 * 템플릿 생성/단건 조회에 사용하는 DTO입니다.
 */
data class PostTemplateInfoDto(
    val id: Long? = null,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
) {
    companion object {
        /** Java 호출 호환을 위한 빌더 팩토리입니다. */
        @JvmStatic
        fun builder(): Builder = Builder()

        /** 엔티티를 DTO로 변환합니다. */
        @JvmStatic
        fun to(template: PostTemplate): PostTemplateInfoDto {
            return PostTemplateInfoDto(
                id = template.id,
                name = template.name,
                title = template.title,
                content = template.content,
            )
        }
    }

    /** Java 테스트/호출 호환을 위한 수동 빌더입니다. */
    class Builder {
        private var id: Long? = null
        private var name: String? = null
        private var title: String? = null
        private var content: String? = null

        fun id(id: Long?): Builder = apply { this.id = id }

        fun name(name: String?): Builder = apply { this.name = name }

        fun title(title: String?): Builder = apply { this.title = title }

        fun content(content: String?): Builder = apply { this.content = content }

        fun build(): PostTemplateInfoDto {
            return PostTemplateInfoDto(
                id = id,
                name = requireNotNull(name) { "name must not be null" },
                title = requireNotNull(title) { "title must not be null" },
                content = requireNotNull(content) { "content must not be null" },
            )
        }
    }
}

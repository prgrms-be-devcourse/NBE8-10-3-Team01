package com.plog.domain.post.dto

import com.plog.domain.post.entity.PostTemplate

/**
 * post template 을 생성하기 위한 데이터 클래스입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
data class PostTemplateInfoDto @JvmOverloads constructor(
    @get:JvmName("id")
    val id: Long? = null,
    @get:JvmName("name")
    val name: String,
    @get:JvmName("title")
    val title: String,
    @get:JvmName("content")
    val content: String
) {
    companion object {
        @JvmStatic
        fun builder(): PostTemplateInfoDtoBuilder {
            return PostTemplateInfoDtoBuilder()
        }

        @JvmStatic
        fun to(template: PostTemplate): PostTemplateInfoDto {
            return PostTemplateInfoDto(
                id = template.id,
                name = template.name,
                title = template.title,
                content = template.content
            )
        }
    }

    class PostTemplateInfoDtoBuilder {
        private var id: Long? = null
        private var name: String? = null
        private var title: String? = null
        private var content: String? = null

        fun id(id: Long?): PostTemplateInfoDtoBuilder {
            this.id = id
            return this
        }

        fun name(name: String?): PostTemplateInfoDtoBuilder {
            this.name = name
            return this
        }

        fun title(title: String?): PostTemplateInfoDtoBuilder {
            this.title = title
            return this
        }

        fun content(content: String?): PostTemplateInfoDtoBuilder {
            this.content = content
            return this
        }

        fun build(): PostTemplateInfoDto {
            return PostTemplateInfoDto(id, name ?: "", title ?: "", content ?: "")
        }
    }
}

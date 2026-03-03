package com.plog.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.plog.domain.post.entity.PostTemplate
import jakarta.validation.constraints.NotBlank

/**
 * post template 을 생성하기 위한 데이터 클래스입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
data class PostTemplateInfoDto(
    @get:JvmName("id")
    @JsonProperty("id")
    val id: Long? = null,

    @get:JvmName("name")
    @JsonProperty("name")
    @field:NotBlank
    val name: String = "",

    @get:JvmName("title")
    @JsonProperty("title")
    @field:NotBlank
    val title: String = "",

    @get:JvmName("content")
    @JsonProperty("content")
    @field:NotBlank
    val content: String = ""
) {
    companion object {
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
}

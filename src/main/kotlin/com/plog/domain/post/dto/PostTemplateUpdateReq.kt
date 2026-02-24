package com.plog.domain.post.dto

import jakarta.validation.constraints.NotBlank

/**
 * postTemplate 을 갱신할 때 사용하는 데이터 클래스입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
data class PostTemplateUpdateReq(
    @get:JvmName("name")
    @field:NotBlank
    val name: String,

    @get:JvmName("title")
    @field:NotBlank
    val title: String,

    @get:JvmName("content")
    @field:NotBlank
    val content: String
)

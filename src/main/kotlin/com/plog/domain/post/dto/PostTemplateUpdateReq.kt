package com.plog.domain.post.dto

import jakarta.validation.constraints.NotBlank

/**
 * 템플릿 수정 요청 DTO입니다.
 */
data class PostTemplateUpdateReq(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
)

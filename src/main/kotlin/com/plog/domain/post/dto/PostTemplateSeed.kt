package com.plog.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 파일을 읽어와, 사용자가 초기에 주어지는 post template seed 를 메모리에 저장할 때 사용합니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
data class PostTemplateSeed(
    @get:JvmName("name")
    @JsonProperty("name")
    val name: String = "",
    @get:JvmName("title")
    @JsonProperty("title")
    val title: String = "",
    @get:JvmName("content")
    @JsonProperty("content")
    val content: String = ""
)

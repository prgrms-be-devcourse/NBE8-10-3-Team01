package com.plog.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.plog.domain.post.entity.PostTemplate

/**
 * 사용자가 본인이 작성한 템플릿을 가져올 때 리스트로서 반환되는 데이터 클래스입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
data class PostTemplateSummaryRes(
    @get:JvmName("name")
    @JsonProperty("name")
    val name: String = "",
    @get:JvmName("id")
    @JsonProperty("id")
    val id: Long? = null
) {
    companion object {
        @JvmStatic
        fun to(postTemplate: PostTemplate): PostTemplateSummaryRes {
            return PostTemplateSummaryRes(
                name = postTemplate.name,
                id = postTemplate.id
            )
        }
    }
}

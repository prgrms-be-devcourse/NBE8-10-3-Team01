package com.plog.domain.post.dto

import com.plog.domain.post.entity.PostTemplate

/**
 * 템플릿 목록 조회 응답용 요약 DTO입니다.
 */
data class PostTemplateSummaryRes(
    val name: String,
    val id: Long,
) {
    companion object {
        /** Java 호출 호환을 위한 빌더 팩토리입니다. */
        @JvmStatic
        fun builder(): Builder = Builder()

        /** 엔티티를 요약 DTO로 변환합니다. */
        @JvmStatic
        fun to(postTemplate: PostTemplate): PostTemplateSummaryRes {
            return PostTemplateSummaryRes(
                name = postTemplate.name,
                id = requireNotNull(postTemplate.id) {
                    "[PostTemplateSummaryRes#to] template id should not be null"
                },
            )
        }
    }

    /** Java 테스트/호출 호환을 위한 수동 빌더입니다. */
    class Builder {
        private var name: String? = null
        private var id: Long? = null

        fun name(name: String?): Builder = apply { this.name = name }

        fun id(id: Long?): Builder = apply { this.id = id }

        fun build(): PostTemplateSummaryRes {
            return PostTemplateSummaryRes(
                name = requireNotNull(name) { "name must not be null" },
                id = requireNotNull(id) { "id must not be null" },
            )
        }
    }
}

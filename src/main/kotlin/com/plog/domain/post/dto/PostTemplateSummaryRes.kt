package com.plog.domain.post.dto

import com.plog.domain.post.entity.PostTemplate

/**
 * 사용자가 본인이 작성한 템플릿을 가져올 때 리스트로서 반환되는 데이터 클래스입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
data class PostTemplateSummaryRes(
    val name: String,
    val id: Long?
) {
    companion object {
        @JvmStatic
        fun builder(): PostTemplateSummaryResBuilder {
            return PostTemplateSummaryResBuilder()
        }

        @JvmStatic
        fun to(postTemplate: PostTemplate): PostTemplateSummaryRes {
            return PostTemplateSummaryRes(
                name = postTemplate.name,
                id = postTemplate.id
            )
        }
    }

    class PostTemplateSummaryResBuilder {
        private var name: String? = null
        private var id: Long? = null

        fun name(name: String?): PostTemplateSummaryResBuilder {
            this.name = name
            return this
        }

        fun id(id: Long?): PostTemplateSummaryResBuilder {
            this.id = id
            return this
        }

        fun build(): PostTemplateSummaryRes {
            return PostTemplateSummaryRes(name ?: "", id)
        }
    }
}

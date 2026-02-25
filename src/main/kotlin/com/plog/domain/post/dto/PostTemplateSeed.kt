package com.plog.domain.post.dto

/**
 * 클래스패스 Markdown 파일에서 읽어 온 기본 템플릿 시드 DTO입니다.
 */
data class PostTemplateSeed(
    val name: String,
    val title: String,
    val content: String,
) {
    companion object {
        /** Java 호출 호환을 위한 빌더 팩토리입니다. */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /** Java 테스트/호출 호환을 위한 수동 빌더입니다. */
    class Builder {
        private var name: String? = null
        private var title: String? = null
        private var content: String? = null

        fun name(name: String?): Builder = apply { this.name = name }

        fun title(title: String?): Builder = apply { this.title = title }

        fun content(content: String?): Builder = apply { this.content = content }

        fun build(): PostTemplateSeed {
            return PostTemplateSeed(
                name = requireNotNull(name) { "name must not be null" },
                title = requireNotNull(title) { "title must not be null" },
                content = requireNotNull(content) { "content must not be null" },
            )
        }
    }
}

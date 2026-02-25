package com.plog.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.plog.domain.post.entity.Post
import java.time.LocalDateTime

/**
 * 게시물 정보 목록을 클라이언트에게 전달하기 위한 응답 데이터 클래스입니다.
 * <p>
 * 데이터베이스 엔티티([Post])를 직접 노출하지 않고,
 * API 스펙에 필요한 필드만을 선택적으로 포함하여 보안성과 유지보수성을 높입니다.
 *
 * <p><b>상속 정보:</b><br>
 * 코틀린 [data class]로 정의되어 불변(Immutable) 상태를 보장합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * [PostListRes] 모든 필드를 초기화하는 생성자를 사용합니다.
 *
 * <p><b>빈 관리:</b><br>
 * 별도의 빈으로 관리되지 않으며, 서비스나 컨트롤러 계층에서 필요 시 생성하여 반환합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 외부 의존성 없이 표준 Java API만을 사용합니다.
 *
 * @author MintyU
 * @see com.plog.domain.post.entity.Post
 * @since 2026-01-28
 */
data class PostListRes(
    @get:JvmName("id")
    @JsonProperty("id")
    val id: Long?,
    @get:JvmName("title")
    @JsonProperty("title")
    val title: String,
    @get:JvmName("summary")
    @JsonProperty("summary")
    val summary: String?,
    @get:JvmName("viewCount")
    @JsonProperty("viewCount")
    val viewCount: Int,
    @get:JvmName("createDate")
    @JsonProperty("createDate")
    val createDate: LocalDateTime?,
    @get:JvmName("modifyDate")
    @JsonProperty("modifyDate")
    val modifyDate: LocalDateTime?,
    @get:JvmName("hashtags")
    @JsonProperty("hashtags")
    val hashtags: List<String>? = null,
    @get:JvmName("thumbnail")
    @JsonProperty("thumbnail")
    val thumbnail: String? = null,
    @get:JvmName("nickname")
    @JsonProperty("nickname")
    val nickname: String,
    @get:JvmName("profileImage")
    @JsonProperty("profileImage")
    val profileImage: String? = null
) {
    companion object {
        /**
         * Post 엔티티 객체를 PostResponse DTO로 변환하는 정적 팩토리 메서드입니다.
         *
         * @param post 변환 대상 엔티티
         * @return 필드값이 매핑된 PostListRes 객체
         */
        @JvmStatic
        fun from(post: Post): PostListRes {
            return PostListRes(
                id = post.id,
                title = post.title,
                summary = post.summary,
                viewCount = post.viewCount,
                createDate = post.createDate,
                modifyDate = post.modifyDate,
                hashtags = post.postHashTags.map { it.displayName },
                thumbnail = post.thumbnail,
                nickname = post.member?.nickname ?: "",
                profileImage = post.member?.profileImage?.accessUrl
            )
        }
    }
}

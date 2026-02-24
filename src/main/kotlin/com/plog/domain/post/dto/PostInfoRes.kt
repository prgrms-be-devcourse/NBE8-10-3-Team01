package com.plog.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.plog.domain.comment.dto.CommentInfoRes
import com.plog.domain.hashtag.entity.PostHashTag
import com.plog.domain.post.entity.Post
import org.springframework.data.domain.Slice
import java.time.LocalDateTime

/**
 * 게시물 상세 정보를 클라이언트에게 전달하기 위한 응답 데이터 클래스입니다.
 * <p>
 * 데이터베이스 엔티티([Post])를 직접 노출하지 않고,
 * API 스펙에 필요한 필드만을 선택적으로 포함하여 보안성과 유지보수성을 높입니다.
 *
 * <p><b>상속 정보:</b><br>
 * 코틀린 [data class]로 정의되어 불변(Immutable) 상태를 보장합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * [PostInfoRes] 모든 필드를 초기화하는 생성자를 사용합니다.
 *
 * <p><b>빈 관리:</b><br>
 * 별도의 빈으로 관리되지 않으며, 서비스나 컨트롤러 계층에서 필요 시 생성하여 반환합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 외부 의존성 없이 표준 Java API만을 사용합니다.
 *
 * @author MintyU
 * @see com.plog.domain.post.entity.Post
 * @since 2026-01-16
 */
data class PostInfoRes(
    @get:JvmName("id")
    @JsonProperty("id")
    val id: Long?,
    @get:JvmName("title")
    @JsonProperty("title")
    val title: String,
    @get:JvmName("content")
    @JsonProperty("content")
    val content: String,
    @get:JvmName("viewCount")
    @JsonProperty("viewCount")
    val viewCount: Int,
    @get:JvmName("createDate")
    @JsonProperty("createDate")
    val createDate: LocalDateTime?,
    @get:JvmName("modifyDate")
    @JsonProperty("modifyDate")
    val modifyDate: LocalDateTime?,
    @get:JvmName("comments")
    @JsonProperty("comments")
    val comments: Slice<CommentInfoRes>? = null,
    @get:JvmName("hashtags")
    @JsonProperty("hashtags")
    val hashtags: List<String>? = null,
    @get:JvmName("thumbnail")
    @JsonProperty("thumbnail")
    val thumbnail: String? = null,
    @get:JvmName("authorid")
    @JsonProperty("authorid")
    val authorid: Long?,
    @get:JvmName("nickname")
    @JsonProperty("nickname")
    val nickname: String,
    @get:JvmName("profileImage")
    @JsonProperty("profileImage")
    val profileImage: String? = null
) {
    companion object {
        /**
         * Post 엔티티 객체를 PostInfoRes DTO로 변환하는 정적 팩토리 메서드입니다.
         *
         * @param post 변환 대상 엔티티
         * @return 필드값이 매핑된 PostInfoRes 객체
         */
        @JvmStatic
        fun from(post: Post): PostInfoRes {
            return from(post, null)
        }

        /**
         * Post 엔티티와 댓글 목록을 받아 PostInfoRes DTO로 변환하는 정적 팩토리 메서드입니다.
         *
         * @param post     변환 대상 엔티티
         * @param comments 게시물에 속한 댓글 슬라이스 데이터
         * @return 필드값과 댓글 목록이 매핑된 PostInfoRes 객체
         */
        @JvmStatic
        fun from(post: Post, comments: Slice<CommentInfoRes>?): PostInfoRes {
            return PostInfoRes(
                id = post.id,
                title = post.title,
                content = post.content,
                viewCount = post.viewCount,
                createDate = post.createDate,
                modifyDate = post.modifyDate,
                comments = comments,
                hashtags = post.postHashTags.map { it.displayName },
                thumbnail = post.thumbnail,
                authorid = post.member?.id,
                nickname = post.member?.nickname ?: "",
                profileImage = post.member?.profileImage?.accessUrl
            )
        }
    }
}

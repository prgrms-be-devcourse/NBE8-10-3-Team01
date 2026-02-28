package com.plog.domain.comment.dto

import com.plog.domain.comment.entity.Comment
import org.springframework.data.domain.Slice
import java.time.LocalDateTime

/**
 * 게시물 댓글 조회 시 클라이언트에게 전달되는 댓글 응답 DTO입니다.
 *
 * 게시물에 속한 댓글의 식별 정보와 내용을 포함하여
 * 댓글 목록 조회 API의 응답 데이터로 사용된다.
 *
 * 주요 생성자:
 * {@link #CommentInfoRes(Comment, Slice<ReplyInfoRes>)}
 * 댓글 엔티티 {@link Comment}를 기반으로
 * 클라이언트 응답에 필요한 데이터만을 매핑하여 생성한다.
 *
 *외부 모듈:
 * Java Time API ({@link java.time.LocalDateTime})를 사용하여
 * 댓글의 생성 및 수정 시점을 표현한다.
 *
 * @author 노정원
 * @see Comment
 * @since 2026-02-23
 */
data class CommentInfoRes(
    val id : Long,
    val content : String,
    val authorId : Long,
    val nickname : String,
    val email : String,
    val profileUrl : String?,
    val postId : Long,
    val createDate : LocalDateTime,
    val modifyDate : LocalDateTime,
    val replyCount : Long,
    val likeCount : Long,
    val previewReplies : Slice<ReplyInfoRes>
    ) {

    companion object {
        fun from(comment: Comment, previewReplies: Slice<ReplyInfoRes>): CommentInfoRes {
            return CommentInfoRes(
                id = comment.id!!,
                content = comment.content,
                authorId = comment.author.id!!,
                nickname = comment.author.nickname,
                email = comment.author.email,
                profileUrl = comment.author.profileImage?.accessUrl,
                postId = comment.post.id!!,
                createDate = comment.createDate!!,
                modifyDate = comment.modifyDate!!,
                replyCount = comment.replyCount,
                likeCount = comment.likeCount,
                previewReplies = previewReplies
            )
        }
    }
}
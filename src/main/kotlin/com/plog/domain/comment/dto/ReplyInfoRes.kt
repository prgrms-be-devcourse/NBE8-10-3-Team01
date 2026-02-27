package com.plog.domain.comment.dto

import com.plog.domain.comment.entity.Comment
import java.time.LocalDateTime

/**
 * 대댓글의 조회 시 클라이언트에게 전달되는 응답 DTO입니다.
 *
 * 주요 생성자:
 * {@link #ReplyInfoRes(Comment)}
 * 댓글 엔티티 {@link Comment}를 기반으로
 * 클라이언트 응답에 필요한 데이터만을 매핑하여 생성한다.
 *
 * 외부 모듈:
 * {@link java.time.LocalDateTime}을 사용하여
 * 대댓글 생성 시각과 수정 시각을 표현한다.
 *
 * @author 노정원
 * @see Comment
 * @since 2026-02-23
 */
data class ReplyInfoRes(
    val id: Long,
    var content: String,
    val parentCommentId: Long,
    val authorId: Long,
    val nickname: String,
    val email: String,
    val profileUrl: String?,
    val likesCount: Long,
    val createDate: LocalDateTime,
    val modifyDate: LocalDateTime,
    ){
    companion object{
        fun from(comment: Comment): ReplyInfoRes {
            return ReplyInfoRes(
                id = comment.id!!,
                content = comment.content,
                parentCommentId = comment.parent?.id!!,
                authorId = comment.author.id!!,
                nickname = comment.author.nickname,
                email = comment.author.email,
                profileUrl = comment.author.profileImage?.accessUrl,
                likesCount = comment.likeCount,
                createDate = comment.createDate!!,
                modifyDate = comment.modifyDate!!
            )
        }
    }
}

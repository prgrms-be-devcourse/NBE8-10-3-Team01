package com.plog.domain.comment.repository

import com.plog.domain.comment.entity.Comment
import com.plog.domain.comment.entity.CommentLike
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 댓글 좋아요(CommentLike) 엔티티에 대한 데이터 액세스 기능을 제공합니다.
 * * 주요 역할:
 * - 특정 사용자의 댓글 좋아요 여부 확인
 * - 좋아요 이력(Member-Comment 매핑) 저장 및 삭제
 * - 중복 좋아요 방지를 위한 이력 조회
 *
 * @author 노정원
 * @since 2026-02-25
 * @see CommentLike
 * @see Comment
 * @see JpaRepository
 */
interface CommentLikeRepository : JpaRepository<CommentLike, Long> {

    fun findByCommentIdAndMemberId(commentId: Long, memberId: Long): CommentLike?
}
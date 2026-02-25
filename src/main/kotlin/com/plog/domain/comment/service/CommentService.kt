package com.plog.domain.comment.service

import com.plog.domain.comment.dto.CommentCreateReq
import com.plog.domain.comment.dto.CommentInfoRes
import com.plog.domain.comment.dto.ReplyInfoRes
import org.springframework.data.domain.Slice
/**
 * 게시글 댓글(Comment) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스.
 *
 * 게시글(Post)에 종속된 댓글 및 대댓글의
 * 생성, 조회(페이징), 수정, 삭제</b> 기능을 추상화한다.
 *
 * 본 인터페이스는 구현체와의 명확한 책임 분리를 통해
 * 서비스 계층의 역할을 드러내고, 테스트 및 확장성을 고려하여 설계되었다.
 *
 * 설계 특징:
 *   댓글과 대댓글을 단일 도메인으로 관리
 *   조회 시 페이징을 적용하여 대량 데이터 처리 성능 고려
 *   Controller 계층에서는 본 인터페이스에만 의존
 *
 *
 * 빈 관리:
 *   구현체는 {@code @Service}로 등록되어 Singleton Bean으로 관리된다.
 *
 * @author njwwn
 * @since 2026-02-24
 */
interface CommentService {
    /**
     * 단일 게시글(Post)에 새로운 댓글 또는 대댓글을 작성한다.
     *
     * [req.parentCommentId]가 null인 경우 일반 댓글로 생성되며,
     * 값이 존재할 경우 해당 댓글을 부모로 하는 대댓글로 생성된다.
     *
     * @param postId 게시글 식별자
     * @param memberId 작성자 식별자
     * @param req 댓글 작성 정보 (내용, 부모 댓글 식별자 등)
     * @return 생성된 댓글의 식별자
     * @throws PostException 게시글이 존재하지 않을 경우
     * @throws CommentException 부모 댓글이 존재하지 않을 경우
     */
    fun createComment(postId: Long, memberId: Long, req: CommentCreateReq): Long

    /**
     * 특정 게시글(Post)에 작성된 루트 댓글 목록을 조회한다. (최대 10개)
     *
     * @param postId 게시글 식별자
     * @param pageNumber 조회할 페이지 번호
     * @return 댓글 목록 응답 DTO 슬라이스
     */
    fun getCommentsByPostId(postId: Long, pageNumber: Int): Slice<CommentInfoRes>

    /**
     * 특정 루트 댓글(Comment)에 작성된 대댓글 목록을 조회한다. (최대 5개)
     *
     * 사용자가 대댓글 보기 이벤트를 발생시킨 경우에만 호출된다.
     *
     * @param commentId 부모 댓글 식별자
     * @param pageNumber 조회할 페이지 번호
     * @return 대댓글 목록 응답 DTO 슬라이스
     * @throws CommentException 부모 댓글이 존재하지 않을 경우
     */
    fun getRepliesByCommentId(commentId: Long, pageNumber: Int): Slice<ReplyInfoRes>

    /**
     * 이미 작성된 댓글(Comment)의 내용을 수정한다.
     *
     * @param commentId 수정할 댓글 식별자
     * @param memberId 작성자 식별자 (권한 검증용)
     * @param content 수정할 내용
     * @throws CommentException 댓글이 존재하지 않거나 수정 권한이 없을 경우
     */
    fun updateComment(commentId: Long, memberId: Long, content: String)

    /**
     * 댓글(Comment)을 삭제 처리한다. (Soft Delete)
     *
     * 자식 댓글이 존재하는 경우 데이터는 유지하되 상태만 삭제로 변경한다.
     *
     * @param commentId 삭제할 댓글 식별자
     * @param memberId 작성자 식별자 (권한 검증용)
     * @throws CommentException 댓글이 존재하지 않거나 삭제 권한이 없을 경우
     */
    fun deleteComment(commentId: Long, memberId: Long)


    fun toggleCommentLike(commentId: Long, memberId: Long): Boolean
}
package com.plog.domain.comment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시물 댓글 생성을 위한 요청 DTO.
 *
 * 특정 게시물에 새로운 댓글을 작성할 때 클라이언트로부터 전달받는 요청 객체이다.
 * 일반 댓글과 대댓글(부모 댓글을 가지는 댓글) 생성을 모두 지원하며,
 * 댓글의 실제 식별 및 계층 구조 처리는 서비스 계층에서 수행된다.
 *
 * 주요 생성자:
 * {@code CreatePostCommentReq(String content, Long parentCommentId)} <br>
 * 댓글 내용과 선택적인 부모 댓글 식별자를 기반으로 요청 객체를 생성한다.
 *
 *
 * 설계 의도:
 * 본 DTO는 요청 데이터 전달만을 책임지며,
 * 게시물 존재 여부 확인, 부모 댓글 유효성 검증,
 * 댓글 계층 구조 설정 등의 비즈니스 로직은
 * 서비스 계층에서 처리한다.
 *
 *
 * @author 노정원
 * @since 2026-02-23
 */
data class CommentCreateReq(
    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 1000, message = "댓글은 1000자 이내로 작성 가능합니다.")
    val content: String,

    val authorId: Long,

    val parentCommentId: Long? = null
)
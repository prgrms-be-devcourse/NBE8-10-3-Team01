package com.plog.domain.comment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시물 댓글 수정을 위한 요청 DTO.
 *
 * 기존에 작성된 댓글의 내용을 수정할 때 클라이언트로부터 전달받는 요청 객체이다.
 * 댓글의 식별자(commentId)는 URL 경로 변수로 전달되며,
 * 본 DTO는 수정할 댓글의 내용만을 포함한다.
 *
 * 주요 생성자:
 * {@code UpdatePostCommentReq(String content)}
 * 수정할 댓글 내용을 기반으로 요청 객체를 생성한다.
 *
 *외부 모듈:
 * Bean Validation({@link jakarta.validation.constraints.NotBlank})을 사용하여
 * 댓글 내용에 대한 유효성 검증을 수행한다.
 *
 * @author 노정원
 * @since 2026-02-23
 */
data class CommentUpdateReq(
    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 1000, message = "댓글은 1000자 이내로 작성 가능합니다.")
    val content: String = ""
)

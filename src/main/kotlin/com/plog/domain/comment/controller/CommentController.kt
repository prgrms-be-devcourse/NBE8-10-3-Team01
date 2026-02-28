package com.plog.domain.comment.controller

import com.plog.domain.comment.dto.CommentCreateReq
import com.plog.domain.comment.dto.CommentInfoRes
import com.plog.domain.comment.dto.CommentUpdateReq
import com.plog.domain.comment.dto.ReplyInfoRes
import com.plog.domain.comment.service.CommentService
import com.plog.global.response.CommonResponse
import com.plog.global.response.Response
import com.plog.global.security.SecurityUser
import jakarta.websocket.server.PathParam
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import java.net.URI

/**
 * 게시물 댓글(Comment)에 대한 REST 컨트롤러.
 *
 * 특정 게시물(postId)에 종속된 댓글의 조회, 생성, 수정, 삭제 기능을 제공한다.
 * 댓글 삭제는 물리 삭제가 아닌 논리 삭제(soft delete) 방식으로 처리되며,
 * 삭제된 댓글은 "[삭제된 댓글입니다.]"라는 대체 메시지로 관리된다.
 *
 * 댓글 조회 : 최대 10개의 댓글이 페이징되어 조회된다.
 * 대댓글 조회 : 최대 5개의 댓글이 페이징되어 조회된다.
 *
 * 주요 생성자:
 * {@code PostCommentController(PostCommentService postCommentService)}<br>
 * 댓글 관련 비즈니스 로직을 수행하는 Service를 주입받는다.
 *
 * 빈 관리:
 * {@link RestController}와 {@link RequiredArgsConstructor}를 통해
 * Spring 컨테이너에 의해 싱글톤 빈으로 관리된다.
 *
 * 외부 모듈:
 * Spring Web MVC, Bean Validation을 사용한다.
 *
 * @author 노정원
 * @see CommentService
 * @since 2026-02-24
 */

@RestController
@RequestMapping("/api")
class CommentController(
    private val commentService: CommentService,
) {

    @GetMapping("posts/{postId}/comments")
    fun getComments(
        @PathVariable postId: Long,
        @RequestParam(name = "pageNumber", defaultValue = "0") pageNumber: Int,
        @AuthenticationPrincipal securityUser: SecurityUser?
    ): ResponseEntity<Response<Slice<CommentInfoRes>>> {
        val commentList = commentService.getCommentsByPostId(postId, pageNumber)

        return ResponseEntity.ok(CommonResponse.success(commentList, "댓글 조회 성공"))
    }

    @GetMapping("/comments/{commentId}/replies")
    fun getReplies(
        @PathVariable commentId: Long,
        @RequestParam(name = "pageNumber", defaultValue = "0") pageNumber: Int,
        @AuthenticationPrincipal securityUser: SecurityUser?
    ): ResponseEntity<Response<Slice<ReplyInfoRes>>> {

        val replyList = commentService.getRepliesByCommentId(commentId, pageNumber)

        return ResponseEntity.ok(CommonResponse.success(replyList, "답글 조회 성공"))
    }

    @PostMapping("/posts/{postId}/comments")
    fun createComment(
        @PathVariable postId: Long,
        @Valid @RequestBody req: CommentCreateReq,
        @AuthenticationPrincipal securityUser: SecurityUser
    ) : ResponseEntity<Void> {

        val commentId = commentService.createComment(postId, securityUser.id, req)

        return ResponseEntity
            .created(URI.create("/api/posts/$postId/comments/$commentId"))
            .build()
    }

    @PutMapping("/comments/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @Valid @RequestBody req: CommentUpdateReq,
        @AuthenticationPrincipal securityUser: SecurityUser
    ) : ResponseEntity<CommonResponse<Long>> {

        commentService.updateComment(commentId,securityUser.id, req.content)

        return ResponseEntity.ok(
            CommonResponse.success(commentId, "댓글 수정 완료")
        )
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal securityUser: SecurityUser
    ) : ResponseEntity<CommonResponse<Long>> {

        commentService.deleteComment(commentId, securityUser.id)

        return ResponseEntity.ok(
            CommonResponse.success(commentId, "댓글 삭제 완료")
        )
    }

    @PostMapping("/comments/{commentId}")
    fun toggleLike(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<CommonResponse<Long>>{

        val likeStatus = commentService.toggleCommentLike(commentId, securityUser.id)

        return ResponseEntity.ok(
            CommonResponse.success(commentId, if(likeStatus) "좋아요" else "좋아요 취소")
        )
    }

}
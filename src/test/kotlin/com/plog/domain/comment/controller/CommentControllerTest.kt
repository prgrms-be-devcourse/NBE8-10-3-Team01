package com.plog.domain.comment.controller

import com.plog.domain.comment.dto.CommentCreateReq
import com.plog.domain.comment.dto.CommentUpdateReq
import com.plog.domain.comment.service.CommentService
import com.plog.global.exception.errorCode.CommentErrorCode
import com.plog.global.exception.exceptions.CommentException
import com.plog.global.security.SecurityUser
import com.plog.testUtil.SecurityTestConfig
import com.plog.testUtil.WebMvcTestSupport
import com.plog.testUtil.WithCustomMockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * 댓글 관련 API(CommentController)에 대한 통합 슬라이싱 테스트.
 */
@WebMvcTest(CommentController::class)
@Import(SecurityTestConfig::class)
@ActiveProfiles("test")
@WithCustomMockUser
class CommentControllerTest : WebMvcTestSupport() {

    @MockitoBean lateinit var commentService: CommentService

    // 공통 테스트 데이터
    private val memberId = 1L
    private val securityUser = SecurityUser(
        id = memberId,
        email = "test@example.com",
        password = "password",
        nickname = "테스터",
        authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
    )

    @Test
    @DisplayName("댓글 생성 성공: 201 Created를 반환한다")
    fun createComment_Success() {
        // 1. 준비
        val postId = 1L
        val req = CommentCreateReq("테스트 댓글", null)

        whenever(commentService.createComment(eq(postId), eq(memberId), any()))
            .thenReturn(100L)

        // 3. 실행 및 검증
        mockMvc.perform(
            post("/api/posts/$postId/comments")
                .with(user(securityUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/posts/$postId/comments/100"))
    }

    @Test
    @DisplayName("댓글 생성 실패: 1000자 초과 시 400 Bad Request를 반환한다")
    fun createComment_Fail_Validation() {
        val postId = 1L
        val longContent = "a".repeat(1001)
        val req = CommentCreateReq(longContent, null)

        mockMvc.perform(
            post("/api/posts/$postId/comments")
                .with(user(securityUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
            .andDo(print())
    }

    @Test
    @DisplayName("댓글 수정 성공: 200 OK를 반환한다")
    fun updateComment_Success() {
        val commentId = 100L
        val req = CommentUpdateReq("수정된 댓글 내용입니다.")

        doNothing().whenever(commentService).updateComment(eq(commentId), eq(memberId), any())

        mockMvc.perform(
            put("/api/comments/$commentId")
                .with(user(securityUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andDo(print())
    }

    @Test
    @DisplayName("댓글 삭제 성공: 200 OK와 성공 메시지를 반환한다")
    fun deleteComment_Success() {
        val commentId = 100L

        doNothing().whenever(commentService).deleteComment(eq(commentId), eq(memberId))

        mockMvc.perform(
            delete("/api/comments/$commentId")
                .with(user(securityUser)).with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("댓글 삭제 완료"))
            .andExpect(jsonPath("$.data").value(commentId))
            .andDo(print())
    }

    @Test
    @DisplayName("댓글 조회 실패: 존재하지 않는 댓글 수정 시 404 Not Found를 반환한다")
    fun updateComment_Fail_NotFound() {
        val commentId = 999L
        val req = CommentUpdateReq("내용")

        whenever(commentService.updateComment(eq(commentId), any(), any()))
            .thenThrow(CommentException(CommentErrorCode.COMMENT_NOT_FOUND, "Not Found", "존재하지 않는 댓글입니다."))

        mockMvc.perform(
            put("/api/comments/$commentId")
                .with(user(securityUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."))
    }

    @Test
    @DisplayName("대댓글 조회 성공: 슬라이싱 파라미터(offset)가 정상 전달된다")
    fun getReplies_Success() {
        val commentId = 1L

        mockMvc.perform(
            get("/api/comments/$commentId/replies")
                .param("offset", "0")
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andDo(print())
    }
}

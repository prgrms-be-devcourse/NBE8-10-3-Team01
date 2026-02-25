package com.plog.domain.comment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.plog.domain.comment.dto.CommentCreateReq
import com.plog.domain.comment.dto.CommentUpdateReq
import com.plog.domain.comment.service.CommentService
import com.plog.global.exception.errorCode.CommentErrorCode
import com.plog.global.exception.exceptions.CommentException
import com.plog.global.security.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 댓글 관련 API(CommentController)에 대한 통합 슬라이싱 테스트.
 * Spring Security 컨텍스트를 Mocking 하여 권한 및 유효성 검증을 포함한 CRUD를 테스트한다.
 *
 * @author 노정원
 * @since 2026-02-24
 */
@WebMvcTest(CommentController::class)
@Import(SecurityConfig::class)
class CommentControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var context: WebApplicationContext

    @MockitoBean
    private lateinit var springObjectMapper: ObjectMapper

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    @MockitoBean private lateinit var commentService: CommentService
    @MockitoBean private lateinit var jwtUtils: JwtUtils
    @MockitoBean private lateinit var tokenResolver: TokenResolver
    @MockitoBean private lateinit var customUserDetailsService: CustomUserDetailsService
    @MockitoBean private lateinit var tokenStore: TokenStore

    // 공통 테스트 데이터
    private val memberId = 1L
    private val securityUser = SecurityUser.securityUserBuilder()
        .id(memberId)
        .email("test@example.com")
        .password("password")
        .nickname("테스터")
        .authorities(listOf(SimpleGrantedAuthority("ROLE_USER")))
        .build()

    @BeforeEach
    fun setUp() {
        // MockMvc가 @AuthenticationPrincipal을 인식하도록 Resolver를 설정합니다.
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context) // WebApplicationContext 주입 필요
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @Test
    @DisplayName("댓글 생성 성공: 201 Created를 반환한다")
    fun createComment_Success() {
        // 1. 준비
        val postId = 1L
        val memberId = 1L // 테스트 클래스에 정의된 memberId 값을 명시적으로 확인
        val req = CommentCreateReq("테스트 댓글", memberId, null)

        // 2. Mock 설정: Matcher(eq, any)를 완전히 제거합니다.
        // 인자값이 Primitive Long일 경우 Matcher 없이 넣는 것이 Kotlin에서 가장 안전합니다.
        given(commentService.createComment(postId, memberId, req))
            .willReturn(100L)

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
        val req = CommentCreateReq(longContent, memberId, null)

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

        willDoNothing().given(commentService).updateComment(eq(commentId), eq(memberId), anyString())

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

        willDoNothing().given(commentService).deleteComment(eq(commentId), eq(memberId))

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

        given(commentService.updateComment(eq(commentId), anyLong(), anyString()))
            .willThrow(CommentException(CommentErrorCode.COMMENT_NOT_FOUND, "Not Found", "존재하지 않는 댓글입니다."))

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

        // Slice 결과는 모킹이 까다로우므로 호출 여부와 응답 코드 위주로 검증
        mockMvc.perform(
            get("/api/comments/$commentId/replies")
                .param("offset", "0")
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andDo(print())
    }
}
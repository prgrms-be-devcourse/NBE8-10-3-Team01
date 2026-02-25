// src/test/kotlin/com/plog/domain/member/controller/AuthControllerTest.kt
package com.plog.domain.member.controller

import com.plog.domain.member.dto.AuthSignInReq
import com.plog.domain.member.dto.AuthSignUpReq
import com.plog.domain.post.service.PostTemplateService
import com.plog.testUtil.WebMvcTestSupport
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * [AuthController] 에 대한 슬라이스 테스트 입니다.
 *
 * @author minhee
 * @since 2026-01-21
 */
@WebMvcTest(AuthController::class)
class AuthControllerTest : WebMvcTestSupport() {

    @MockitoBean
    private lateinit var postTemplateService: PostTemplateService

    @Test
    @DisplayName("회원가입 성공 - 201, Location 헤더를 반환")
    fun signUp_success() {
        // given
        val memberId = 1L
        val req = AuthSignUpReq(
            email = "test@plog.com",
            password = "password123!",
            nickname = "plogger"
        )

        given(authService.signUp(any())).willReturn(memberId)

        // when
        val result = mockMvc.perform(
            post("/api/members/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )

        // then
        result.andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/members/$memberId"))
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 올바르지 않음, 400 에러 반환.")
    fun signUp_fail_invalidEmail() {
        // given
        val req = AuthSignUpReq(
            email = "invalid-email",
            password = "password123!",
            nickname = "plogg"
        )

        // when
        val result = mockMvc.perform(
            post("/api/members/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )

        // then
        result.andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 공백, 400 에러 반환")
    fun signIn_fail_emptyEmail() {
        // given
        val req = AuthSignInReq(
            email = "",
            password = "password123!"
        )

        // when & then
        mockMvc.perform(
            post("/api/members/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("로그아웃 성공 - 200, 쿠키 삭제 확인")
    fun logout_success() {
        // given
        val refreshToken = "refresh-token"
        given(tokenResolver.resolveRefreshToken(any())).willReturn(refreshToken)

        // when
        val result = mockMvc.perform(
            get("/api/members/logout")
                .contentType(MediaType.APPLICATION_JSON)
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."))

        // 쿠키가 삭제되었는지 검증
        verify(authService).logout(eq(refreshToken))
        verify(tokenResolver).deleteRefreshTokenCookie(any())
    }
}
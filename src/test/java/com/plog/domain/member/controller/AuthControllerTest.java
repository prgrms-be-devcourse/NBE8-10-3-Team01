package com.plog.domain.member.controller;


import com.plog.domain.member.service.AuthService;
import com.plog.global.rq.Rq;
import com.plog.global.security.JwtUtils;
import com.plog.testUtil.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *  {@link AuthController} 에 대한 슬라이스 테스트 입니다.
 *
 * @author minhee
 * @since 2026-01-21
 */

@WebMvcTest(AuthController.class)
public class AuthControllerTest extends WebMvcTestSupport {

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private Rq rq;
    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("회원가입 성공 시 201 상태코드와 Location 헤더를 반환한다.")
    void signUp_success() throws Exception {
        // given
        Long memberId = 1L;
        AuthController.MemberSignUpReq req = new AuthController.MemberSignUpReq(
                "test@plog.com",
                "password123!",
                "plogger"
        );

        given(authService.signUp(anyString(), anyString(), anyString())).willReturn(memberId);

        // when
        ResultActions result = mockMvc.perform(post("/api/members/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/members/" + memberId));
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 에러를 반환한다.")
    void signUp_fail_invalidEmail() throws Exception {
        // given
        AuthController.MemberSignUpReq req = new AuthController.MemberSignUpReq(
                "invalid-email",
                "password123!",
                "plogg"
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/members/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isBadRequest());
    }
}
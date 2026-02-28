package com.plog.global.security.oauth2.handler

import com.plog.global.security.JwtUtils
import com.plog.global.security.SecurityUser
import com.plog.global.security.TokenResolver
import com.plog.global.security.TokenStore
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.security.core.Authentication
import org.springframework.security.web.RedirectStrategy
import org.springframework.test.util.ReflectionTestUtils

/**
 * [OAuth2SuccessHandler]에 대한 단위 테스트입니다.
 *
 * @author minhee
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension::class)
class OAuth2SuccessHandlerTest {

    @Mock
    private lateinit var jwtUtils: JwtUtils

    @Mock
    private lateinit var tokenResolver: TokenResolver

    @Mock
    private lateinit var tokenStore: TokenStore

    @Mock
    private lateinit var redirectStrategy: RedirectStrategy

    private lateinit var oauth2SuccessHandler: OAuth2SuccessHandler

    private val allowedOrigins = "http://localhost:3000"

    @BeforeEach
    fun setUp() {
        oauth2SuccessHandler = OAuth2SuccessHandler(
            jwtUtils,
            tokenResolver,
            tokenStore,
            allowedOrigins
        )
        // SimpleUrlAuthenticationSuccessHandler 내부의 redirectStrategy를 Mock으로 교체
        ReflectionTestUtils.setField(oauth2SuccessHandler, "redirectStrategy", redirectStrategy)
    }

    @Test
    @DisplayName("로그인 성공 시 토큰을 생성하여 쿠키에 저장하고 프론트엔드로 리다이렉트한다")
    fun onAuthenticationSuccess_success() {
        // given
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val authentication = mock(Authentication::class.java)
        val user = mock(SecurityUser::class.java)

        val accessToken = "mock-access-token"
        val refreshToken = "mock-refresh-token"
        val userEmail = "test@plog.com"

        given(authentication.principal).willReturn(user)
        given(user.id).willReturn(1L)
        given(user.email).willReturn(userEmail)
        given(user.nickname).willReturn("plogger")

        given(jwtUtils.createAccessToken(any())).willReturn(accessToken)
        given(jwtUtils.createRefreshToken(userEmail)).willReturn(refreshToken)

        // when
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

        // then
        // AT, RT 쿠키가 모두 설정되었는지 확인
        verify(tokenResolver).setAccessTokenCookie(response, accessToken)
        verify(tokenResolver).setRefreshTokenCookie(response, refreshToken)

        // TokenStore에 RefreshToken이 저장되었는지 확인
        verify(tokenStore).save(userEmail, refreshToken)

        // 첫 번째 allowedOrigin 주소로 리다이렉트가 일어났는지 확인
        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:3000")
    }
}
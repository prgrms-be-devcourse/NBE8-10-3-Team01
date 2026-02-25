// src/test/kotlin/com/plog/global/security/TokenResolverTest.kt
package com.plog.global.security

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * [TokenResolver]의 토큰 추출 및 설정 로직을 검증하는 단위 테스트입니다.
 *
 * @author minhee
 * @since 2026-01-23
 */
class TokenResolverTest {

    private lateinit var tokenResolver: TokenResolver

    /**
     * 테스트용 설정값입니다.
     */
    private val EXPIRATION = 3600000L
    private val DOMAIN = "localhost"
    private val SECURE = false

    @BeforeEach
    fun setUp() {
        tokenResolver = TokenResolver(EXPIRATION, DOMAIN, SECURE)
    }

    @Test
    @DisplayName("Access Token 추출 - Authorization 헤더에 Bearer 토큰이 있으면 접두사를 제거하고 반환한다")
    fun resolveAccessToken_success() {
        // given
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer mock-token-value")

        // when
        val result = tokenResolver.resolveAccessToken(request)

        // then
        assertThat(result).isEqualTo("mock-token-value")
    }

    @Test
    @DisplayName("Refresh Token 추출 - 쿠키에 refreshToken이 존재하면 값을 반환한다")
    fun resolveRefreshToken_success() {
        // given
        val request = MockHttpServletRequest()
        val cookie = Cookie("refreshToken", "refresh-token-value")
        request.setCookies(cookie)

        // when
        val result = tokenResolver.resolveRefreshToken(request)

        // then
        assertThat(result).isEqualTo("refresh-token-value")
    }

    @Test
    @DisplayName("토큰 응답 설정 - 헤더에는 Bearer AT가, 쿠키에는 HttpOnly RT가 설정되어야 한다")
    fun setTokenResponse_test() {
        // given
        val response = MockHttpServletResponse()
        val access = "new-access"
        val refresh = "new-refresh"

        // when
        tokenResolver.setHeader(response, access)
        tokenResolver.setCookie(response, refresh)

        // then
        assertThat(response.getHeader("Authorization")).isEqualTo("Bearer new-access")

        val cookie = response.getCookie("refreshToken")
        assertThat(cookie).isNotNull
        assertThat(cookie!!.value).isEqualTo("new-refresh")
        assertThat(cookie.isHttpOnly).isTrue
        assertThat(cookie.path).isEqualTo("/")
    }

    @Test
    @DisplayName("추출 실패 케이스 - 토큰이 없으면 null을 반환한다")
    fun resolve_fail_returnsNull() {
        // given
        val request = MockHttpServletRequest()

        // when & then
        assertThat(tokenResolver.resolveAccessToken(request)).isNull()
        assertThat(tokenResolver.resolveRefreshToken(request)).isNull()
    }
}
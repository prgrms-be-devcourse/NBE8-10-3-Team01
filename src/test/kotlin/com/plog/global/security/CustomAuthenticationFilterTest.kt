// src/test/kotlin/com/plog/global/security/CustomAuthenticationFilterTest.kt
package com.plog.global.security

import com.plog.domain.member.dto.MemberInfoRes
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * [CustomAuthenticationFilter]의 인증 처리 및 자동 토큰 재발급을 검증하는 테스트입니다.
 *
 * 유효한 토큰을 통한 인증 성공 케이스와 Access Token 만료 시 Refresh Token을 활용한
 * 재발급 과정이 설계된 대로 동작하는지 확인합니다.
 *
 * @author minhee
 * @since 2026-01-23
 */
@ExtendWith(MockitoExtension::class)
class CustomAuthenticationFilterTest {

    @InjectMocks
    private lateinit var filter: CustomAuthenticationFilter

    @Mock
    private lateinit var jwtUtils: JwtUtils

    @Mock
    private lateinit var tokenResolver: TokenResolver

    @Mock
    private lateinit var customUserDetailsService: CustomUserDetailsService

    @Mock
    private lateinit var tokenStore: TokenStore

    @Test
    @DisplayName("자동 재발급 - AT 만료 시 RT가 화이트리스트에 존재하면 새 토큰을 설정한다")
    fun handleAccessTokenReissue_success() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = mock(FilterChain::class.java)

        val expiredAt = "expired_access_token"
        val validRt = "valid_refresh_token"
        val newAt = "new_access_token"
        val email = "test@plog.com"
        val password = "password123!"

        // Access Token 추출 및 만료 예외 발생 시뮬레이션
        given(tokenResolver.resolveAccessToken(request)).willReturn(expiredAt)
        given(jwtUtils.parseToken(expiredAt)).willThrow(ExpiredJwtException::class.java)

        // Refresh Token 처리 로직 모킹
        given(tokenResolver.resolveRefreshToken(request)).willReturn(validRt)
        val rtClaims = mock(Claims::class.java)
        given(rtClaims.subject).willReturn(email)
        given(jwtUtils.parseToken(validRt)).willReturn(rtClaims)
        given(tokenStore.get(email)).willReturn(validRt)

        // 서비스 및 유틸리티 모킹
        val user = SecurityUser(
            id = 1L,
            email = email,
            nickname = "plogger",
            password = password,
            authorities = emptyList()
        )
        given(customUserDetailsService.loadUserByUsername(email)).willReturn(user)
        given(jwtUtils.createAccessToken(any<MemberInfoRes>())).willReturn(newAt)

        // when
        filter.doFilter(request, response, filterChain)

        // then
        verify(tokenResolver).setHeader(response, newAt)
        verify(filterChain).doFilter(request, response)
    }
}
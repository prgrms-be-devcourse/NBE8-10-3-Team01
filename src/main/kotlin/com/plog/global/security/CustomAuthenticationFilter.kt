// src/main/kotlin/com/plog/global/security/CustomAuthenticationFilter.kt
package com.plog.global.security

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.global.exception.errorCode.AuthErrorCode
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

/**
 * 모든 HTTP 요청에 대해 JWT 토큰의 유효성을 검사하는 인증 필터입니다.
 *
 * 요청 헤더에서 Access Token을 추출하여 검증합니다. 만약 Access Token이 만료되었다면
 * 쿠키의 Refresh Token과 [TokenStore](Caffeine)를 대조하여 Access Token을 자동으로 재발급합니다.
 *
 * **상속 정보:**
 * [OncePerRequestFilter]를 상속받아 하나의 요청당 단 한 번만 실행됨을 보장합니다.
 *
 * **주요 생성자:**
 * `CustomAuthenticationFilter(JwtUtils, TokenResolver, CustomUserDetailsService, TokenStore)`
 * 인증 유지 및 토큰 재발급에 필요한 보안 컴포넌트들을 주입받습니다.
 *
 * **빈 관리:**
 * [Component] 어노테이션을 통해 스프링 빈으로 등록되며,
 * SecurityConfig에서 필터 체인의 적절한 위치에 수동으로 등록됩니다.
 *
 * **외부 모듈:**
 * Spring Security Core 및 Servlet API를 사용합니다.
 *
 * @author minhee
 * @see SecurityContextHolder
 * @see JwtUtils
 * @since 2026-01-16
 */
@Component
class CustomAuthenticationFilter(
    private val jwtUtils: JwtUtils,
    private val tokenResolver: TokenResolver,
    private val customUserDetailsService: CustomUserDetailsService,
    private val tokenStore: TokenStore
) : OncePerRequestFilter() {

    /**
     * 필터의 핵심 로직을 수행하며, Access Token의 유효성을 검사하고 만료 시 재발급을 시도합니다.
     */
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessToken = tokenResolver.resolveAccessToken(request)

        try {
            if (accessToken != null) {
                authenticate(accessToken)
            }
        } catch (e: ExpiredJwtException) {
            handleAccessTokenReissue(request, response)
        } catch (e: Exception) {
            request.setAttribute("exception", AuthErrorCode.TOKEN_INVALID)
        }

        filterChain.doFilter(request, response)
    }

    /**
     * 검증된 [SecurityUser] 정보를 바탕으로 Spring Security 인증 객체를 생성하여 컨텍스트에 등록합니다.
     * @param user 인증된 사용자 정보 객체
     */
    private fun processAuthentication(user: SecurityUser) {
        val auth = UsernamePasswordAuthenticationToken(user, null, user.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }

    /**
     * 전달받은 Access Token을 파싱하여 Spring Security 인증 객체를 생성하고 컨텍스트에 등록합니다.
     *
     * 토큰의 Claims에서 사용자의 PK(id), 식별자(email), 닉네임을 추출하여 [SecurityUser]를 구성합니다.
     *
     * @param token 파싱할 JWT Access Token 문자열
     */
    private fun authenticate(token: String) {
        val claims = jwtUtils.parseToken(token)
        val id = claims.get("id", java.lang.Long::class.java)?.toLong()
            ?: throw IllegalArgumentException("Token id claim is missing")
        val email = claims.subject
        val nickname = claims.get("nickname", String::class.java)

        val user = SecurityUser(
            id = id,
            email = email,
            password = "",
            nickname = nickname ?: "",
            authorities = emptyList()
        )

        processAuthentication(user)
    }

    /**
     * Access Token 만료 시 자동 재발급을 수행합니다.
     *
     * 쿠키의 Refresh Token과 [TokenStore]에 저장된 토큰이 일치하는지 확인합니다.
     * 일치할 경우 새 Access Token을 생성하여 응답 헤더에 담고 인증을 승인합니다.
     * 검증 실패(만료, 불일치 등) 시 관련 쿠키를 제거하고 로그인 유도 예외를 설정합니다.
     *
     * @param request  RefreshToken 추출용
     * @param response 새 토큰 전달 및 쿠키 삭제용
     */
    private fun handleAccessTokenReissue(request: HttpServletRequest, response: HttpServletResponse) {
        val refreshToken = tokenResolver.resolveRefreshToken(request)

        if (refreshToken == null) {
            request.setAttribute("exception", AuthErrorCode.LOGIN_REQUIRED)
            return
        }

        try {
            val claims = jwtUtils.parseToken(refreshToken)
            val email = claims.subject
            val savedToken = tokenStore.get(email)

            if (savedToken == null || savedToken != refreshToken) {
                tokenResolver.deleteRefreshTokenCookie(response)
                request.setAttribute("exception", AuthErrorCode.LOGIN_REQUIRED)
                return
            }

            val user = customUserDetailsService.loadUserByUsername(email) as SecurityUser

            val memberInfo = MemberInfoRes(
                id = user.id,
                email = user.email,
                nickname = user.nickname
            )

            val newAccess = jwtUtils.createAccessToken(memberInfo)
            tokenResolver.setHeader(response, newAccess)

            processAuthentication(user)
        } catch (e: ExpiredJwtException) {
            tokenResolver.deleteRefreshTokenCookie(response)
            request.setAttribute("exception", AuthErrorCode.LOGIN_REQUIRED)
        } catch (e: Exception) {
            request.setAttribute("exception", AuthErrorCode.TOKEN_INVALID)
        }
    }
}
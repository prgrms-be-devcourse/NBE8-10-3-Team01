// src/main/kotlin/com/plog/global/security/LoginFilter.kt
package com.plog.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.plog.domain.member.dto.AuthInfoRes
import com.plog.domain.member.dto.AuthSignInReq
import com.plog.domain.member.dto.MemberInfoRes
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.response.CommonResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.util.StreamUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * 사용자 로그인을 처리하고 JWT 토큰을 발급하는 필터입니다.
 *
 * `/api/members/sign-in` 경로로 JSON 바디를 통한 로그인을 처리하며,
 * 인증 성공 시 Access Token과 Refresh Token을 반환하고 Refresh Token을 서버 측 [TokenStore](Caffeine)에 기록하여 세션을 관리합니다.
 *
 * **상속 정보:**
 * [UsernamePasswordAuthenticationFilter]를 상속받아 인증 시도 및 성공/실패 로직을 재정의합니다.
 *
 * **주요 생성자:**
 * `LoginFilter(AuthenticationManager, ObjectMapper, JwtUtils, ...)`
 *  * 인증 처리를 위한 매니저와 토큰 생성 유틸리티, 쿠키 설정을 위한 외부 환경 변수를 주입받습니다.
 *
 * **빈 관리:**
 * [SecurityConfig]에서 `@Bean` 대신 직접 인스턴스화하여 필터 체인에 등록됩니다.
 *
 * **외부 모듈:**
 * Jackson [ObjectMapper]를 통해 JSON 요청을 파싱합니다.
 *
 * @author minhee
 * @see com.plog.global.security.SecurityConfig
 * @since 2026-01-22
 */
class LoginFilter(
    private val authenticationManager: AuthenticationManager,
    private val objectMapper: ObjectMapper,
    private val jwtUtils: JwtUtils,
    private val tokenResolver: TokenResolver,
    private val tokenStore: TokenStore
) : UsernamePasswordAuthenticationFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        setFilterProcessesUrl("/api/members/sign-in")
    }

    /**
     * HTTP 요청 바디에서 이메일과 비밀번호를 추출하여 인증을 시도합니다.
     *
     * @param request  HTTP 요청 (JSON 바디 포함)
     * @param response HTTP 응답
     * @return 인증 객체 [Authentication]
     * @throws AuthenticationException 인증 실패 시 발생
     */
    @Throws(AuthenticationException::class)
    override fun attemptAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Authentication {
        try {
            val body = StreamUtils.copyToString(request.inputStream, StandardCharsets.UTF_8)
            val authSignInReq = objectMapper.readValue(body, AuthSignInReq::class.java)
            val authRequest = UsernamePasswordAuthenticationToken(
                authSignInReq.email,
                authSignInReq.password
            )
            return authenticationManager.authenticate(authRequest)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * 인증 성공 시 호출되며, 토큰을 생성하고 서버 저장소에 Refresh Token을 보관합니다.
     *
     * 클라이언트에게 JSON 형태로 성공 메시지와 사용자 정보를 반환합니다.
     */
    @Throws(IOException::class, ServletException::class)
    override fun successfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
        authentication: Authentication
    ) {
        val user = authentication.principal as SecurityUser

        val memberInfo = MemberInfoRes(
            id = user.id,
            email = user.email,
            nickname = user.nickname
        )

        val accessToken = jwtUtils.createAccessToken(memberInfo)
        val refreshToken = jwtUtils.createRefreshToken(user.email)
        tokenResolver.setHeader(response, accessToken)
        tokenResolver.setCookie(response, refreshToken)
        tokenStore.save(user.email, refreshToken)

        response.contentType = "application/json;charset=UTF-8"

        val authInfoRes = AuthInfoRes(
            id = user.id,
            nickname = user.nickname,
            accessToken = accessToken
        )

        response.writer.write(
            objectMapper.writeValueAsString(
                CommonResponse.success(
                    authInfoRes,
                    "${authInfoRes.nickname}님 환영합니다."
                )
            )
        )
    }

    /**
     * 인증에 실패했을 때 실행되며, 공통 에러 응답 형식을 반환합니다.
     */
    @Throws(IOException::class, ServletException::class)
    override fun unsuccessfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
        failed: AuthenticationException
    ) {
        log.info("[LoginFilter#unsuccessfulAuthentication] Login failed for user: {}", failed.message)

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                CommonResponse.fail<Any>(AuthErrorCode.INVALID_CREDENTIALS.message)
            )
        )
    }
}
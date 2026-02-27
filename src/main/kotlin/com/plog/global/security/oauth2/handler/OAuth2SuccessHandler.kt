package com.plog.global.security.oauth2.handler

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.global.security.JwtUtils
import com.plog.global.security.SecurityUser
import com.plog.global.security.TokenResolver
import com.plog.global.security.TokenStore
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

/** TODO: 주석 채우기
 * 코드에 대한 전체적인 역할을 적습니다.
 *
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 *
 * **상속 정보:**<br></br>
 * 상속 정보를 적습니다.
 *
 *
 * **주요 생성자:**<br></br>
 * `ExampleClass(String example)`  <br></br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br></br>
 *
 *
 * **빈 관리:**<br></br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 *
 * **외부 모듈:**<br></br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author minhee
 * @see
 * @since 2026-02-27
 */
@Component
class OAuth2SuccessHandler(
    private val jwtUtils: JwtUtils,
    private val tokenResolver: TokenResolver,
    private val tokenStore: TokenStore,
    @Value("\${custom.cors.allowed-origins}") private val allowedOrigins: String
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        // TODO: 중복 로직 분리
        val user = authentication.principal as SecurityUser

        val memberInfo = MemberInfoRes(
            id = user.id,
            email = user.email,
            nickname = user.nickname
        )

        // Header는 리다이렉트 시 유실되기 때문에 쿠키만 세팅해서 보내고 AT는 자동 재발급 처리
        val refreshToken = jwtUtils.createRefreshToken(user.email)
        tokenResolver.setCookie(response, refreshToken)
        tokenStore.save(user.email, refreshToken)

        val frontBaseUrl = allowedOrigins.split(",")[0].trim()
        redirectStrategy.sendRedirect(request, response, frontBaseUrl)
    }
}
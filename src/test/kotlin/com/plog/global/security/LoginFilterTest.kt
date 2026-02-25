// src/test/kotlin/com/plog/global/security/LoginFilterTest.kt
package com.plog.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.plog.domain.member.dto.AuthSignInReq
import com.plog.domain.member.dto.MemberInfoRes
import com.plog.global.exception.errorCode.AuthErrorCode
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
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
import org.mockito.kotlin.argThat
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.test.util.ReflectionTestUtils

/**
 * 로그인 인증을 담당하는 [LoginFilter]의 핵심 로직을 검증하는 테스트 클래스입니다.
 *
 * HTTP 요청 바디에서 JSON 데이터를 추출하여 스프링 시큐리티의 인증 토큰으로 변환하고,
 * [AuthenticationManager]에게 인증을 올바르게 위임하는지 확인합니다.
 *
 * **상속 정보:**
 * 없음. `WebMvcTestSupport`를 상속하지 않은 이유는 해당 유틸이 `@WebMvcTest` 기반의 슬라이스 테스트를 전제로 하기 때문입니다.
 * 필터 내부의 독립적인 로직 검증을 위해 스프링 컨텍스트를 배제했습니다.
 *
 * **주요 생성자:**
 * `LoginFilter(AuthenticationManager, ObjectMapper, JwtUtils, ...)`
 * 테스트 대상인 필터가 필드 주입이 아닌 생성자 주입 방식을 사용하므로,
 * `@BeforeEach` 단계에서 Mock 객체들을 수동으로 주입하여 인스턴스를 생성합니다.
 *
 * **외부 모듈:**
 * Mockito (객체 모킹 및 행위 검증)
 *
 * @author minhee
 * @since 2026-01-23
 */
@ExtendWith(MockitoExtension::class)
class LoginFilterTest {

    private lateinit var loginFilter: LoginFilter

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var jwtUtils: JwtUtils

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())

    // Spy 대신 실제 객체 사용 (TokenResolver 로직이 단순하므로)
    private val tokenResolver = TokenResolver(3600000, "localhost", false)

    @Mock
    private lateinit var tokenStore: TokenStore

    @BeforeEach
    fun setUp() {
        loginFilter = LoginFilter(authenticationManager, objectMapper, jwtUtils, tokenResolver, tokenStore)
    }

    @Test
    @DisplayName("인증 시도 - 요청 바디의 JSON을 AuthSignInReq로 변환하여 인증을 위임")
    fun attemptAuthentication_success() {
        // given
        val request = MockHttpServletRequest()
        val signInReq = AuthSignInReq("test@plog.com", "password123!")
        request.setContent(objectMapper.writeValueAsBytes(signInReq))

        val mockAuthentication = mock(Authentication::class.java)
        given(authenticationManager.authenticate(any())).willReturn(mockAuthentication)

        // when
        loginFilter.attemptAuthentication(request, MockHttpServletResponse())

        // then
        verify(authenticationManager).authenticate(argThat { auth ->
            auth is UsernamePasswordAuthenticationToken &&
                auth.principal == "test@plog.com" &&
                auth.credentials == "password123!"
        })
    }

    @Test
    @DisplayName("인증 성공 - 토큰 생성 및 헤더/쿠키 설정 확인")
    fun successfulAuthentication_test() {
        val name = "plogger"
        val email = "test@plog.com"
        val accessToken = "mock-access-token"
        val refreshToken = "mock-refresh-token"

        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = mock(FilterChain::class.java)

        val securityUser = SecurityUser(
            id = 1L,
            email = email,
            nickname = name,
            password = "",
            authorities = emptyList()
        )

        val authentication = mock(Authentication::class.java)
        given(authentication.principal).willReturn(securityUser)

        given(jwtUtils.createAccessToken(any<MemberInfoRes>())).willReturn(accessToken)
        given(jwtUtils.createRefreshToken(any())).willReturn(refreshToken)

        // when - protected 메서드이기 때문에 Reflection 사용
        ReflectionTestUtils.invokeMethod<Unit>(
            loginFilter,
            "successfulAuthentication",
            request,
            response,
            filterChain,
            authentication
        )

        // then
        verify(tokenStore).save(email, refreshToken)
        assertThat(response.getHeader("Authorization")).isEqualTo("Bearer $accessToken")

        val cookie = response.getCookie("refreshToken")
        assertThat(cookie).isNotNull
        assertThat(cookie!!.value).isEqualTo(refreshToken)
        assertThat(cookie.isHttpOnly).isTrue

        val content = response.contentAsString
        assertThat(content).contains("\"id\":1")
        assertThat(content).contains("${name}님 환영합니다.")
        assertThat(content).contains(accessToken)
    }

    @Test
    @DisplayName("인증 실패 - 401 상태코드와 실패 메시지 반환 확인")
    fun unsuccessfulAuthentication_test() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val failed = mock(AuthenticationException::class.java)

        // when - protected 메서드이기 때문에 Reflection 사용
        ReflectionTestUtils.invokeMethod<Unit>(
            loginFilter,
            "unsuccessfulAuthentication",
            request,
            response,
            failed
        )

        // then
        assertThat(response.status).isEqualTo(401)

        val expectedMessage = AuthErrorCode.INVALID_CREDENTIALS.message
        val content = response.contentAsString
        assertThat(content).contains("fail")
        assertThat(content).contains(expectedMessage)
    }
}
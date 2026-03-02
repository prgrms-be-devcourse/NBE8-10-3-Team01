// src/main/kotlin/com/plog/global/security/SecurityConfig.kt
package com.plog.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.plog.global.security.oauth2.CustomOAuth2UserService
import com.plog.global.security.oauth2.handler.OAuth2SuccessHandler
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.response.CommonResponse
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.io.IOException

/**
 * 애플리케이션의 보안 정책을 설정하는 중앙 설정 클래스입니다.
 *
 * HTTP 요청에 대한 보안 필터 체인을 정의하며, JWT 인증 방식에 맞게
 * 세션 정책을 STATELESS로 설정하고 CORS/CSRF 등 보안 옵션을 조정합니다.
 * 로그인 처리([LoginFilter])와 요청 검증([CustomAuthenticationFilter]) 필터를
 * 등록해 토큰 기반 인증/인가 프로세스를 완성합니다.
 *
 * **상속 정보:**
 * 별도의 상속 없이 Spring Security 6.x 버전의 컴포넌트 기반 설정을 따릅니다.
 *
 * **주요 생성자:**
 * `SecurityConfig(...)`
 * 인증 관리에 필요한 매니저, 토큰 생성/해석 유틸리티, 외부 설정(CORS) 등을 주입받아 초기화합니다.
 *
 * **빈 관리:**
 * [Configuration]으로 등록되어 보안 관련 Bean(SecurityFilterChain 등)을 생성 및 관리합니다.
 *
 * **외부 모듈:**
 * Spring Security 등을 사용하여 보안 기능을 구현합니다.
 *
 * @author minhee
 * @since 2026-01-16
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val objectMapper: ObjectMapper,
    private val jwtUtils: JwtUtils,
    private val authenticationConfiguration: AuthenticationConfiguration,
    @Value("\${custom.cors.allowed-origins}") private val allowedOrigins: List<String>,
    private val tokenResolver: TokenResolver,
    private val tokenStore: TokenStore,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler
) {

    @Throws(Exception::class)
    private fun loginFilter(): LoginFilter {
        return LoginFilter(
            authenticationManager(authenticationConfiguration),
            objectMapper,
            jwtUtils,
            tokenResolver,
            tokenStore
        )
    }

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.GET, *AccessURL.GET_PUBLIC.urls.toTypedArray()).permitAll()
                    .requestMatchers(*AccessURL.PUBLIC.urls.toTypedArray()).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oAuth2SuccessHandler)
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .csrf { it.disable() } // REST API이므로 CSRF 비활성화
            .formLogin { it.disable() }
            .logout { it.disable() }
            .httpBasic { it.disable() } // 기본 로그인창 비활성화
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            } // 세션 미사용
            .addFilterAt(loginFilter(), UsernamePasswordAuthenticationFilter::class.java) // 로그인 처리 필터
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java) // JWT 필터
            .exceptionHandling { exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(this::handleAuthEntryPoint)
                    .accessDeniedHandler(this::handleAccessDenied)
            }

        return http.build()
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }

    /**
     * 인증되지 않은 사용자가 보호된 리소스에 접근했을 때의 처리를 담당합니다.
     * [CustomAuthenticationFilter]에서 설정한 "exception" 속성을 확인하여
     * JWT 만료, 유효하지 않은 토큰 등의 구체적인 인증 실패 원인을 파악합니다.
     *
     * **결과 반환:**
     * - 속성이 없을 경우: [AuthErrorCode.LOGIN_REQUIRED] (401 Unauthorized) 반환
     * - 구체적 에러가 있을 경우: 해당 에러 코드에 맞는 HTTP 상태와 메시지를 JSON 형식으로 반환
     */
    @Throws(IOException::class, ServletException::class)
    private fun handleAuthEntryPoint(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.contentType = "application/json;charset=UTF-8"
        var errorCode = request.getAttribute("exception") as? AuthErrorCode
        if (errorCode == null) {
            errorCode = AuthErrorCode.LOGIN_REQUIRED
        }

        response.status = errorCode.httpStatus.value()
        response.writer.write(
            objectMapper.writeValueAsString(
                CommonResponse.fail<Any>(errorCode.message)
            )
        )
    }

    /**
     * 권한이 없는 사용자가 리소스에 접근했을 때의 처리를 담당합니다.
     *
     * **역할:**
     * 인가(Authorization) 실패 시 클라이언트에게 권한 부족을 알리는 공통 응답을 생성합니다.
     *
     * **결과 반환:**
     * HTTP 403 Forbidden 상태 코드와 함께 [AuthErrorCode.USER_AUTH_FAIL] 메시지를 JSON으로 반환합니다.
     */
    @Throws(IOException::class, ServletException::class)
    private fun handleAccessDenied(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.contentType = "application/json;charset=UTF-8"
        response.status = HttpStatus.FORBIDDEN.value()
        response.writer.write(
            objectMapper.writeValueAsString(
                CommonResponse.fail<Any>(AuthErrorCode.USER_AUTH_FAIL.message)
            )
        )
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 정의하는 Bean입니다.
     * 허용 오리진: 외부 설정 파일(application.yml)을 통해 주입받습니다.
     * @return 설정이 완료된 CORS 구성 소스 객체
     * @see CorsConfiguration
     */
    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration()

        // 허용할 오리진 설정 -> 필요 시 수정
        configuration.allowedOrigins = allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")

        // 자격 증명 허용 설정
        configuration.allowCredentials = true

        // 허용할 헤더 설정
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("Authorization", "Set-Cookie")

        // CORS 설정을 소스에 등록
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        source.registerCorsConfiguration("/login/oauth2/**", configuration)

        return source
    }
}
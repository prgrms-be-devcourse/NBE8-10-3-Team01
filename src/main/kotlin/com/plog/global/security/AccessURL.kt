// src/main/kotlin/com/plog/global/security/AccessURL.kt
package com.plog.global.security

/**
 * 애플리케이션 보안 설정을 위한 URL 관리 열거형입니다.
 *
 * Spring Security의 필터 체인에서 인증 과정 없이 접근을 허용할(permitAll) 경로들을
 * 그룹화하여 관리합니다. 기본적으로 모든 요청을 차단(Deny by Default)하는 보안 정책 하에,
 * 예외적으로 허용할 엔드포인트를 정의하는 역할을 합니다.
 *
 * **상속 정보:**
 * [Enum]을 상속받아 고정된 경로 그룹 상수를 정의합니다.
 *
 * **주요 생성자:**
 * `AccessURL(List<String> urls)`
 * 보안 설정에 사용될 경로 패턴 리스트를 주입받아 초기화합니다.
 *
 * **빈 관리:**
 * Spring Context에 빈으로 등록되지 않으며, `SecurityConfig` 클래스에서
 * 정적 참조를 통해 보안 설정 구성을 돕습니다.
 *
 * **외부 모듈:**
 * Spring Security의 `authorizeHttpRequests` 설정 시 `requestMatchers`의
 * 인자로 사용됩니다.
 *
 * @author minhee
 * @since 2026-01-20
 */
enum class AccessURL(val urls: List<String>) {
    /** 인증 없이 모든 사용자에게 노출되는 공개 API 경로입니다. */
    PUBLIC(
        listOf(
            "/api/members/sign-up",
            "/api/members/sign-in",
            "/api/members/logout"
        )
    ),

    /** 인증 없이 GET 요청을 받아야 하는 공개 API 경로입니다. */
    GET_PUBLIC(
        listOf(
            "/api/posts/**"
        )
    )
}
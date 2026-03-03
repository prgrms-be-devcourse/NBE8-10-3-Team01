// src/main/kotlin/com/plog/global/security/SecurityUser.kt
package com.plog.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * Spring Security의 인증 인터페이스를 구현한 사용자 정의 인증 객체입니다.
 *
 * 시스템 내에서 인증된 사용자의 정보를 세션이나 SecurityContext에 보관하며,
 * 사용자의 식별자(id), 이메일, 닉네임 등의 기본 정보를 제공합니다.
 *
 * **상속 정보:**
 * [User] 클래스를 상속받아 구현되었습니다.
 *
 * **주요 생성자:**
 * 빌더 패턴을 통해 생성되며, 부모 클래스인 User에 인증 정보를 전달합니다.
 *
 * **빈 관리:**
 * UserDetailsService 구현체에서 사용자 정보를 조회하여 인스턴스를 생성합니다.
 *
 * **외부 모듈:**
 * Spring Security Core 모듈을 활용합니다.
 *
 * @author minhee
 * @see org.springframework.security.core.userdetails.UserDetails
 * @since 2026-01-16
 */
class SecurityUser(
    val id: Long,
    email: String,
    password: String = "",
    val nickname: String,
    authorities: Collection<GrantedAuthority>,
    private val attributes: Map<String, Any> = emptyMap()
) : User(email, password, authorities), OAuth2User {

    /**
     * 부모 클래스(User)의 username 필드(이메일)를 반환하는 getter
     */
    val email: String
        get() = super.getUsername()

    override fun getAttributes(): Map<String, Any> = attributes
    override fun getName(): String = email
}

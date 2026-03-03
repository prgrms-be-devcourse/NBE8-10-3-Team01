// src/main/kotlin/com/plog/global/security/SecurityUser.kt
package com.plog.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * Spring Security의 인증 인터페이스를 구현한 사용자 정의 인증 객체입니다.
 *
 * OAuth2(Kakao, Naver)와 OIDC(Google) 로그인을 모두 처리할 수 있도록
 * [OAuth2User]와 [OidcUser] 인터페이스를 동시에 구현합니다.
 * OIDC 전용 필드([idToken], [oidcUserInfo])는 nullable로 선언되어,
 * OAuth2 로그인 시에는 null로 유지됩니다.
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
    private val attributes: Map<String, Any> = emptyMap(),
    private val idToken: OidcIdToken? = null,
    private val oidcUserInfo: OidcUserInfo? = null
) : User(email, password, authorities), OAuth2User, OidcUser {

    override fun getAttributes(): Map<String, Any> = attributes
    override fun getName(): String = email

    // OidcUser
    override fun getEmail(): String = super.getUsername()
    override fun getClaims(): Map<String, Any> = idToken?.claims ?: attributes
    override fun getUserInfo(): OidcUserInfo? = oidcUserInfo
    override fun getIdToken(): OidcIdToken? = idToken
}
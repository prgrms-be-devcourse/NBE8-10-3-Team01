// src/main/kotlin/com/plog/global/security/SecurityUser.kt
package com.plog.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

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
    password: String,
    val nickname: String,
    authorities: Collection<GrantedAuthority>
) : User(email, password, authorities) {

    /**
     * 부모 클래스(User)의 username 필드(이메일)를 반환하는 getter
     */
    val email: String
        get() = super.getUsername()

    // TODO: 마이그레이션 완료 후 삭제 필요
    companion object {
        @JvmStatic
        fun securityUserBuilder(): SecurityUserBuilder {
            return SecurityUserBuilder()
        }
    }

    /**
     * 자바 호환성을 위한 수동 빌더 클래스
     */
    class SecurityUserBuilder {
        private var id: Long? = null
        private var email: String? = null
        private var password: String? = null
        private var nickname: String? = null
        private var authorities: Collection<GrantedAuthority>? = null

        fun id(id: Long): SecurityUserBuilder {
            this.id = id
            return this
        }

        fun email(email: String): SecurityUserBuilder {
            this.email = email
            return this
        }

        fun password(password: String): SecurityUserBuilder {
            this.password = password
            return this
        }

        fun nickname(nickname: String): SecurityUserBuilder {
            this.nickname = nickname
            return this
        }

        fun authorities(authorities: Collection<GrantedAuthority>): SecurityUserBuilder {
            this.authorities = authorities
            return this
        }

        fun build(): SecurityUser {
            return SecurityUser(
                id = id ?: throw IllegalArgumentException("id is required"),
                email = email ?: throw IllegalArgumentException("email is required"),
                password = password ?: throw IllegalArgumentException("password is required"),
                nickname = nickname ?: throw IllegalArgumentException("nickname is required"),
                authorities = authorities ?: emptyList()
            )
        }
    }
}
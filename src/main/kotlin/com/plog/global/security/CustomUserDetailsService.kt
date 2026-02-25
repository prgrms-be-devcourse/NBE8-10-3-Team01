// src/main/kotlin/com/plog/global/security/CustomUserDetailsService.kt
package com.plog.global.security

import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Spring Security 인증 과정에서 사용자 정보를 조회하는 핵심 서비스 클래스입니다.
 *
 * 전달받은 사용자 식별자(Email)를 기반으로 데이터베이스에서 회원 정보를 탐색하고,
 * Spring Security가 인식할 수 있는 형태인 [SecurityUser] 인스턴스를 생성하여 반환합니다.
 *
 * **상속 정보:**
 * [UserDetailsService] 인터페이스를 상속받아 구현되었습니다.
 *
 * **주요 생성자:**
 * `CustomUserDetailsService(MemberRepository memberRepository)`
 * 생성자 주입을 통해 DB 접근을 위한 MemberRepository를 주입받습니다.
 *
 * **빈 관리:**
 * [Service] 어노테이션에 의해 Spring 빈으로 등록되며,
 * 인증 관리자(AuthenticationManager)가 사용자 유효성 검증 시 이 빈을 참조합니다.
 *
 * **외부 모듈:**
 * Spring Security Core 및 Spring Data JPA 라이브러리를 사용합니다.
 *
 * @author minhee
 * @since 2026-01-16
 * @see SecurityUser
 * @see UserDetailsService
 */
@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): UserDetails {
        val member = memberRepository.findByEmail(email)
            .orElseThrow {
                AuthException(
                    AuthErrorCode.USER_NOT_FOUND,
                    "[CustomUserDetailsService#loadUserByUsername] can't find user by email: $email",
                    "존재하지 않는 사용자입니다."
                )
            }

        return SecurityUser(
            id = member.id!!, // TODO: BaseEntity 마이그레이션 후 !! 삭제
            email = member.email,
            password = member.password,
            nickname = member.nickname,
            authorities = emptyList() // 나중에 권한(Role)이 필요하면 여기에 추가
        )
    }
}
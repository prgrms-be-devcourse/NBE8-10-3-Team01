// src/main/kotlin/com/plog/domain/member/service/AuthServiceImpl.kt
package com.plog.domain.member.service

import com.plog.domain.member.dto.AuthSignUpReq
import com.plog.domain.member.dto.MemberInfoRes
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.security.JwtUtils
import com.plog.global.security.TokenStore
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [AuthService] 인터페이스의 구현체로, 인증 및 인가와 관련된 실제 비즈니스 로직을 수행합니다.
 *
 * `BCryptPasswordEncoder`를 이용한 비밀번호 암호화와 [JwtUtils]를 활용한
 * 토큰 기반 인증 시스템을 구축합니다. 모든 예외 상황은 [AuthException]을 통해 관리됩니다.
 *
 * **상속 정보:**
 * [AuthService] 인터페이스의 자식 구현 클래스
 *
 * **외부 모듈:**
 * 1. Spring Security Crypto (PasswordEncoder)
 * 2. JJWT (io.jsonwebtoken)
 *
 * @author minhee
 * @see AuthService
 * @since 2026-01-15
 */
@Service
@Transactional(readOnly = true)
class AuthServiceImpl(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtils: JwtUtils,
    private val tokenStore: TokenStore
) : AuthService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun signUp(req: AuthSignUpReq): Long {
        if (isDuplicateEmail(req.email)) {
            throw AuthException(
                AuthErrorCode.USER_ALREADY_EXIST,
                "[AuthServiceImpl#signUp] email dup",
                "이미 가입된 이메일입니다."
            )
        }

        if (isDuplicateNickname(req.nickname)) {
            throw AuthException(
                AuthErrorCode.USER_ALREADY_EXIST,
                "[AuthServiceImpl#signUp] user dup",
                "이미 사용 중인 닉네임입니다."
            )
        }

        val encodedPassword = passwordEncoder.encode(req.password)!!
        val member = Member(
            email = req.email,
            password = encodedPassword,
            nickname = req.nickname
        )
        // TODO: 맞는지 확인
        return memberRepository.save(member).id ?: throw IllegalStateException("Member ID should not be null after save")
    }

    override fun logout(refreshToken: String?) {
        refreshToken?.let { token ->
            try {
                val email = jwtUtils.parseToken(token).subject
                tokenStore.delete(email)
            } catch (e: Exception) {
                // TODO: throw 말고 log로 처리? 확인하기
                log.info("이미 만료되었거나 유효하지 않은 토큰으로 로그아웃 시도")
            }
        }
    }

    /**
     * 이메일 중복 여부를 확인합니다.
     *
     * @return 이미 사용 중인 이메일이면 `true`, 아니면 `false`
     * @throws IllegalArgumentException email 이 null 이거나 빈 값인 경우
     */
    private fun isDuplicateEmail(email: String): Boolean {
        return memberRepository.existsByEmail(email)
    }

    /**
     * 닉네임 중복 여부를 확인합니다.
     *
     * @param nickname 중복 여부를 확인할 닉네임
     * @return 이미 사용 중인 닉네임이면 `true`, 아니면 `false`
     * @throws IllegalArgumentException nickname 이 null 이거나 빈 값인 경우
     */
    private fun isDuplicateNickname(nickname: String): Boolean {
        return memberRepository.existsByNickname(nickname)
    }

    /**
     * 회원 ID를 기준으로 사용자 정보를 조회합니다.
     *
     * @param id 조회할 회원의 고유 식별자
     * @return 조회된 회원 정보
     * @throws IllegalArgumentException id가 null 인 경우
     * @throws AuthException 해당 ID에 대한 회원이 존재하지 않는 경우
     */
    override fun findMemberWithId(id: Long): MemberInfoRes {
        val member = memberRepository.findById(id)
            .orElseThrow {
                AuthException(
                    AuthErrorCode.USER_NOT_FOUND,
                    "[AuthServiceImpl#findMemberWithId] can't find user by id",
                    "존재하지 않는 사용자입니다."
                )
            }

        return MemberInfoRes.from(member)
    }
}
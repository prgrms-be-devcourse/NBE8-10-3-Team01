// src/test/kotlin/com/plog/domain/member/service/AuthServiceTest.kt
package com.plog.domain.member.service

import com.plog.domain.member.dto.AuthSignUpReq
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.security.JwtUtils
import com.plog.global.security.TokenStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

/**
 * [AuthServiceImpl] 에 대한 단위 테스트 입니다.
 *
 * @author minhee
 * @since 2026-01-21
 */
@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtUtils: JwtUtils

    @Mock
    private lateinit var tokenStore: TokenStore

    @InjectMocks
    private lateinit var authService: AuthServiceImpl

    @Test
    @DisplayName("회원가입 성공 - 이메일/닉네임 중복 없음, 비밀번호를 암호화, ID 반환")
    fun signUp_success() {
        // given
        val req = AuthSignUpReq("test@plog.com", "password123!", "plogger")
        val encodedPassword = "encoded_password"

        val mockMember = mock(Member::class.java)
        given(mockMember.id).willReturn(1L)

        given(memberRepository.existsByEmail(req.email)).willReturn(false)
        given(memberRepository.existsByNickname(req.nickname)).willReturn(false)
        given(passwordEncoder.encode(req.password)).willReturn(encodedPassword)
        given(memberRepository.save(any(Member::class.java))).willReturn(mockMember)

        // when
        val savedId = authService.signUp(req)

        // then
        assertThat(savedId).isEqualTo(1L)
        then(memberRepository).should().save(any(Member::class.java))
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    fun signUp_fail_alreadyExist() {
        // given
        val email = "exist@plog.com"
        val req = AuthSignUpReq(email, "pw", "nick")
        given(memberRepository.existsByEmail(email)).willReturn(true)

        // when
        val ex = assertThrows(AuthException::class.java) {
            authService.signUp(req)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_ALREADY_EXIST)
        assertThat(ex.message).isEqualTo("이미 가입된 이메일입니다.")
        verify(memberRepository, never()).save(any(Member::class.java))
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 사용 중인 닉네임")
    fun signUp_fail_alreadyExistNickname() {
        // given
        val email = "test@plog.com"
        val nickname = "duplicateNick"
        val req = AuthSignUpReq(email, "pw", nickname)

        given(memberRepository.existsByEmail(email)).willReturn(false)
        given(memberRepository.existsByNickname(nickname)).willReturn(true)

        // when
        val ex = assertThrows(AuthException::class.java) {
            authService.signUp(req)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_ALREADY_EXIST)
        assertThat(ex.message).isEqualTo("이미 사용 중인 닉네임입니다.")
        then(passwordEncoder).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("회원 ID 조회 성공 - 존재하는 ID일 경우 정보를 반환")
    fun findMemberWithId_success() {
        // given
        val memberId = 1L
        val member = Member(
            email = "test@plog.com",
            password = "password",
            nickname = "plogger"
        )

        ReflectionTestUtils.setField(member, "id", memberId)
        ReflectionTestUtils.setField(member, "createDate", java.time.LocalDateTime.now())
        ReflectionTestUtils.setField(member, "modifyDate", java.time.LocalDateTime.now())

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member))

        // when
        val result = authService.findMemberWithId(memberId)

        // then
        assertThat(result.id).isEqualTo(memberId)
        assertThat(result.email).isEqualTo("test@plog.com")
    }

    @Test
    @DisplayName("회원 ID 조회 실패 - 존재하지 않는 ID일 경우 AuthException 발생")
    fun findMemberWithId_fail() {
        // given
        val invalidId = 999L
        given(memberRepository.findById(invalidId)).willReturn(Optional.empty())

        // when
        val ex = assertThrows(AuthException::class.java) {
            authService.findMemberWithId(invalidId)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        assertThat(ex.logMessage).contains("[AuthServiceImpl#findMemberWithId]")
    }

    @Test
    @DisplayName("로그아웃 시도 - 토큰이 만료되었거나 유효하지 않아도 예외 없이 종료")
    fun logout_fail_invalidToken() {
        // given
        val invalidToken = "invalid_token"
        given(jwtUtils.parseToken(invalidToken)).willThrow(RuntimeException("parsing fail"))

        // when
        authService.logout(invalidToken)

        // then
        verify(tokenStore, never()).delete(anyString())
    }

    @Test
    @DisplayName("로그아웃 시도 - 토큰이 null이면 아무 작업도 수행하지 않음")
    fun logout_nullToken() {
        // when
        authService.logout(null)

        // then
        verify(jwtUtils, never()).parseToken(anyString())
        verify(tokenStore, never()).delete(anyString())
    }
}
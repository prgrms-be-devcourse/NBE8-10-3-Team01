// src/test/kotlin/com/plog/global/security/CustomUserDetailsServiceTest.kt
package com.plog.global.security

import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

/**
 * [CustomUserDetailsService]에 대한 단위 테스트입니다.
 *
 * DB에서 사용자 정보를 조회하여 스프링 시큐리티 전용 객체인 [SecurityUser]로
 * 올바르게 변환하는지 검증합니다.
 *
 * **테스트 전략:**
 * 컨벤션에 따라 스프링 컨텍스트를 로드하지 않고 Mockito만을 사용하여
 * 레포지토리와의 의존성을 분리한 순수 단위 테스트를 수행합니다.
 *
 * @author minhee
 * @since 2026-01-23
 */
@ExtendWith(MockitoExtension::class)
class CustomUserDetailsServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @InjectMocks
    private lateinit var userDetailsService: CustomUserDetailsService

    @Test
    @DisplayName("사용자 조회 성공 - 이메일이 존재하면 SecurityUser를 반환")
    fun loadUserByUsername_success() {
        // given
        val email = "test@plog.com"
        val member = Member(
            email = email,
            password = "encoded_password",
            nickname = "plogger"
        )
        ReflectionTestUtils.setField(member, "id", 1L)
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member))

        // when
        val userDetails = userDetailsService.loadUserByUsername(email) as SecurityUser

        // then
        assertThat(userDetails.username).isEqualTo(email)
        assertThat(userDetails.nickname).isEqualTo("plogger")
        assertThat(userDetails.id).isEqualTo(1L)
        assertThat(userDetails.password).isEqualTo("encoded_password")
    }

    @Test
    @DisplayName("사용자 조회 실패 - 이메일이 없으면 AuthException(USER_NOT_FOUND)이 발생")
    fun loadUserByUsername_fail_notFound() {
        // given
        val email = "non-exist@plog.com"
        given(memberRepository.findByEmail(email)).willReturn(Optional.empty())

        // when
        val ex = assertThrows(AuthException::class.java) {
            userDetailsService.loadUserByUsername(email)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        assertThat(ex.logMessage).contains("[CustomUserDetailsService#loadUserByUsername]")
        assertThat(ex.message).isEqualTo("존재하지 않는 사용자입니다.")
    }
}
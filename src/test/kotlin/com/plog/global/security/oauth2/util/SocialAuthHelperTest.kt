package com.plog.global.security.oauth2.util

import com.plog.domain.member.entity.Member
import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.member.repository.SocialAuthRepository
import com.plog.domain.member.util.RandomNicknameGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

/**
 * [SocialAuthHelper]의 핵심 비즈니스 로직(가입, 연동, 닉네임 생성)을 검증하는 테스트입니다.
 * @author minhee
 * @since 2026-03-02
 * @see
 */

@ExtendWith(MockitoExtension::class)
class SocialAuthHelperTest {

    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var socialAuthRepository: SocialAuthRepository
    @Mock private lateinit var nicknameGenerator: RandomNicknameGenerator

    @InjectMocks
    private lateinit var socialAuthHelper: SocialAuthHelper

    @Test
    @DisplayName("신규 가입 - 닉네임 중복 시 최대 5번 재시도 후 UUID를 붙여 저장한다")
    fun createNewMember_retry_and_save() {
        // given
        val email = "new@test.com"
        val duplicateNick = "nick"
        whenever(nicknameGenerator.generate()).thenReturn(duplicateNick)
        whenever(memberRepository.existsByNickname(any())).thenReturn(true)
        whenever(memberRepository.save(any())).thenAnswer { it.arguments[0] as Member }

        // when
        val result = socialAuthHelper.createNewMember(email)

        // then
        verify(nicknameGenerator, times(5)).generate()
        assertThat(result.nickname).startsWith("${duplicateNick}_")
        assertThat(result.email).isEqualTo(email)
        verify(memberRepository).save(any())
    }

    @Test
    @DisplayName("기존 이메일 존재 시 - 소셜 정보만 추가하고 기존 멤버를 반환한다")
    fun findOrCreateMember_existingEmail() {
        // given
        val email = "exist@test.com"
        val existingMember = Member(email = email, nickname = "old", password = "p")
        whenever(socialAuthRepository.findByProviderAndProviderId(any(), any())).thenReturn(null)
        whenever(memberRepository.findByEmail(email)).thenReturn(existingMember)

        // when
        val result = socialAuthHelper.findOrCreateMember(email, SocialAuthProvider.KAKAO, "kakao-123")

        // then
        assertThat(result).isEqualTo(existingMember)
        verify(socialAuthRepository).save(any())
        verify(memberRepository, never()).save(any())
    }
}
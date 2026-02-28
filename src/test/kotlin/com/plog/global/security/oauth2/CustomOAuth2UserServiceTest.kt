package com.plog.global.security.oauth2

import com.plog.domain.member.entity.Member
import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.member.repository.SocialAuthRepository
import com.plog.domain.member.util.RandomNicknameGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.test.util.ReflectionTestUtils

/**
 * [CustomOAuth2UserService]의 핵심 비즈니스 로직(가입, 연동, 닉네임 생성)을 검증하는 테스트입니다.
 * @author minhee
 * @since 2026-02-27
 */

@ExtendWith(MockitoExtension::class)
class CustomOAuth2UserServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var socialAuthRepository: SocialAuthRepository

    @Mock
    private lateinit var nicknameGenerator: RandomNicknameGenerator

    @InjectMocks
    private lateinit var customOAuth2UserService: CustomOAuth2UserService

    @Test
    @DisplayName("닉네임 생성 로직 - 닉네임이 중복되면 최대 5번 재시도하고, 계속 중복되면 UUID를 붙인다")
    fun createNewMember_nicknameRetry_success() {
        val email = "new-user@test.com"
        val duplicateNick = "duplicate"

        // given
        given(nicknameGenerator.generate())
            .willReturn(duplicateNick, duplicateNick, duplicateNick, duplicateNick, duplicateNick)
        given(memberRepository.existsByNickname(duplicateNick)).willReturn(true)

        val savedMember = Member(email = email, nickname = "임시_UUID", password = "any")
        ReflectionTestUtils.setField(savedMember, "id", 100L)
        given(memberRepository.save(any())).willReturn(savedMember)

        // when
        val method = CustomOAuth2UserService::class.java.getDeclaredMethod("createNewMember", String::class.java)
        method.isAccessible = true
        val result = method.invoke(customOAuth2UserService, email) as Member

        // then
        verify(nicknameGenerator, times(5)).generate()
        verify(memberRepository).save(any())
        assertThat(result.id).isEqualTo(100L)
    }

    @Test
    @DisplayName("계정 연동 - 동일 이메일 유저가 존재하면 SocialAuth만 추가하고 기존 Member를 반환한다")
    fun findOrCreateMember_linkSocial_success() {
        val email = "existing@test.com"
        val attributes = mock(OAuth2Attributes::class.java)
        val existingMember = Member(email = email, nickname = "기존이름", password = "pw")

        // given
        given(attributes.getProvider()).willReturn(SocialAuthProvider.KAKAO)
        given(attributes.getProviderId()).willReturn("kakao-12345")
        ReflectionTestUtils.setField(existingMember, "id", 1L)

        given(socialAuthRepository.findByProviderAndProviderId(any(), any())).willReturn(null)
        given(memberRepository.findByEmail(email)).willReturn(existingMember)

        // when
        val method = CustomOAuth2UserService::class.java.getDeclaredMethod("findOrCreateMember", OAuth2Attributes::class.java, String::class.java)
        method.isAccessible = true
        val result = method.invoke(customOAuth2UserService, attributes, email) as Member

        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo(email)

        verify(socialAuthRepository).save(any())
        verify(memberRepository, never()).save(any())
    }
}
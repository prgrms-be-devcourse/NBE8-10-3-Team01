package com.plog.global.auth

import com.plog.domain.member.entity.Member
import com.plog.domain.member.entity.SocialAuth
import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.member.repository.SocialAuthRepository
import com.plog.domain.member.util.RandomNicknameGenerator
import com.plog.global.auth.oauth2.OAuth2Attributes
import com.plog.global.security.SecurityUser
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


/** TODO: 주석 채우기
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ExampleClass(String example)}  <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author minhee
 * @since 2026-02-27
 * @see
 */

@Service
class CustomOAuth2UserService(
    private val memberRepository: MemberRepository,
    private val socialAuthRepository: SocialAuthRepository,
    private val nicknameGenerator: RandomNicknameGenerator
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {

        val oAuth2User = super.loadUser(userRequest)

        val registrationId = userRequest.clientRegistration.registrationId
        val provider = SocialAuthProvider.valueOf(registrationId.uppercase())

        val attributes = OAuth2Attributes.of(
            provider = provider,
            userNameAttributeName = userRequest.clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName,
            attributes = oAuth2User.attributes
        )

        val email = attributes.getEmail()
            ?: throw OAuth2AuthenticationException("소셜 계정에서 이메일 정보를 불러올 수 없습니다.")

        val member = findOrCreateMember(attributes, email)

        return SecurityUser(
            id = member.id ?: throw IllegalStateException("회원 가입에 실패했습니다."),
            email = member.email,
            password = "",
            nickname = member.nickname,
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes = attributes.attributes
        )
    }

    /**
     * 유저의 소셜 연동 상태에 따라 적절한 Member를 반환하거나 생성합니다.
     */
    private fun findOrCreateMember(attributes: OAuth2Attributes, email: String): Member {
        val provider = attributes.getProvider()
        val providerId = attributes.getProviderId()

        // 이미 해당 소셜로 연동된 계정이 있는 경우 (로그인)
        socialAuthRepository.findByProviderAndProviderId(provider, providerId)?.let {
            return it.member
        }

        // 소셜 연동은 없으나, 같은 이메일의 기존 회원이 있는 경우 (계정 연동)
        memberRepository.findByEmail(email)?.let { existingMember ->
            createSocialLink(existingMember, provider, providerId)
            return existingMember
        }

        // 소셜 연동도 없고 이메일도 처음인 경우 (신규 가입)
        val newMember = createNewMember(email)
        createSocialLink(newMember, provider, providerId)
        return newMember
    }

    /**
     * 신규 회원 가입을 처리합니다.
     */
    private fun createNewMember(email: String): Member {
        var nickname = ""
        var isSuccess = false

        for (i in 1..5) {
            nickname = nicknameGenerator.generate()
            if (!memberRepository.existsByNickname(nickname)) {
                isSuccess = true
                break
            }
        }

        if (!isSuccess) {
            val prefix = if (nickname.length > 4) nickname.substring(0, 4) else nickname
            nickname = "${prefix}_${UUID.randomUUID().toString().substring(0, 4)}"
        }

        val member = Member(
            email = email,
            nickname = nickname,
            password = UUID.randomUUID().toString() // 임시 비밀번호
        )
        return memberRepository.save(member)
    }

    /**
     * 특정 회원에게 소셜 인증 정보를 연결합니다.
     */
    private fun createSocialLink(member: Member, provider: SocialAuthProvider, providerId: String) {
        val socialAuth = SocialAuth(
            member = member,
            provider = provider,
            providerId = providerId
        )
        socialAuthRepository.save(socialAuth)
    }
}
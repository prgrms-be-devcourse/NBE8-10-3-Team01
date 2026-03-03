package com.plog.global.security.oauth2.util

import com.plog.domain.member.entity.Member
import com.plog.domain.member.entity.SocialAuth
import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.member.repository.SocialAuthRepository
import com.plog.domain.member.util.RandomNicknameGenerator
import com.plog.global.security.oauth2.OAuth2Attributes
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 소셜 로그인(OAuth2, OIDC) 시 사용자 식별 및 회원 가입/연동 처리를 담당하는 헬퍼 컴포넌트입니다.
 * <p>
 * 다양한 소셜 제공자(Google, Kakao 등)로부터 넘어온 정보를 바탕으로 시스템 내의
 * {@link Member} 엔티티와의 연결 고리를 생성하거나 신규 계정을 발급합니다.
 *
 * <p><b>작동 원리:</b><br>
 * 1. <b>기존 연동 확인:</b> 제공자(Provider)와 고유 식별값(ProviderId)으로 이미 연결된 소셜 계정이 있는지 확인합니다.<br>
 * 2. <b>계정 자동 연동:</b> 소셜 연결 정보는 없으나 시스템에 동일한 이메일의 회원이 존재할 경우, 해당 계정에 소셜 정보를 자동으로 연결합니다.<br>
 * 3. <b>신규 가입:</b> 위 두 경우에 해당하지 않을 시, 랜덤 닉네임을 생성하여 신규 회원으로 등록하고 소셜 정보를 연결합니다.<br>
 * 4. <b>닉네임 생성:</b> {@link RandomNicknameGenerator}를 이용하며, 중복 발생 시 UUID를 활용한 폴백(Fallback) 로직을 실행합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code SocialAuthHelper(MemberRepository, SocialAuthRepository, RandomNicknameGenerator)}<br>
 * 회원 관리 및 소셜 인증 관리를 위한 Repository와 닉네임 생성기를 주입받습니다.
 *
 * <p><b>빈 관리:</b><br>
 * {@code @Component}로 선언되어 스프링 컨테이너에 의해 싱글톤 빈으로 관리됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * {@code java.util.UUID}를 사용하여 임시 비밀번호 및 중복 닉네임 방지 처리를 수행합니다.
 *
 * @author minhee
 * @since 2026-03-02
 * @see Member
 * @see SocialAuth
 * @see OAuth2Attributes
 */

@Component
@Transactional(readOnly = true)
class SocialAuthHelper(
    private val memberRepository: MemberRepository,
    private val socialAuthRepository: SocialAuthRepository,
    private val nicknameGenerator: RandomNicknameGenerator
) {
    /**
     * OAuth2 로그인용 - OAuth2Attributes에서 provider, providerId 추출 후 위임
     */
    @Transactional
    fun findOrCreateMember(attributes: OAuth2Attributes, email: String): Member {
        return findOrCreateMember(
            email = email,
            provider = attributes.getProvider(),
            providerId = attributes.getProviderId()
        )
    }

    /**
     * OAuth2, OIDC 양쪽에서 공통으로 사용
     * 유저의 소셜 연동 상태에 따라 적절한 Member를 반환하거나 생성합니다.
     */
    @Transactional
    fun findOrCreateMember(email: String, provider: SocialAuthProvider, providerId: String): Member {

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
    @Transactional
    fun createNewMember(email: String): Member {
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
    @Transactional
    fun createSocialLink(member: Member, provider: SocialAuthProvider, providerId: String) {
        val socialAuth = SocialAuth(
            member = member,
            provider = provider,
            providerId = providerId
        )
        socialAuthRepository.save(socialAuth)
    }
}
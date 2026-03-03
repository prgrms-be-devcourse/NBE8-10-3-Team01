package com.plog.global.security.oauth2.service

import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.security.SecurityUser
import com.plog.global.security.oauth2.OAuth2Attributes
import com.plog.global.security.oauth2.util.SocialAuthHelper
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

/**
 * OAuth2 제공자(Naver, Kakao 등)로부터 받은 사용자 정보를 바탕으로 시스템의 유저를 로드하고 가입시킵니다.
 * <p>
 * 리소스 서버로부터 사용자 프로필을 가져온 후 호출되며, 수신된 데이터를 서비스 도메인 모델({@link SecurityUser})로
 * 변환하여 Spring Security의 인증 프로세스에 전달하는 브릿지 역할을 수행합니다.
 *
 * <p><b>작동 원리:</b><br>
 * 1. {@code super.loadUser}를 호출하여 외부 제공자의 원시 사용자 속성(Attributes)을 획득합니다.<br>
 * 2. {@code registrationId}를 통해 소셜 제공자(Provider)를 식별합니다.<br>
 * 3. {@link OAuth2Attributes}를 사용하여 각 제공자마다 다른 응답 형식을 공통 규격으로 정규화합니다.<br>
 * 4. {@link SocialAuthHelper}를 통해 기존 회원 여부를 확인하거나, 신규 회원일 경우 자동 회원가입을 진행합니다.<br>
 * 5. 최종적으로 서비스 내부에서 사용할 {@link SecurityUser} 객체를 생성하여 반환합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link DefaultOAuth2UserService}를 상속받아 표준 OAuth2 유저 로딩 기능을 확장 및 커스텀합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code CustomOAuth2UserService(SocialAuthHelper socialAuthHelper)}<br>
 * DB 연동 및 회원 가입 비즈니스 로직을 처리하는 {@code socialAuthHelper}를 주입받습니다.
 *
 * <p><b>빈 관리:</b><br>
 * {@code @Service} 어노테이션을 통해 스프링 컨테이너의 서비스 빈으로 등록되어 관리됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Spring Security OAuth2 Client 라이브러리를 기반으로 동작합니다.
 *
 * @author minhee
 * @since 2026-02-27
 * @see SocialAuthHelper
 * @see SecurityUser
 * @see OAuth2Attributes
 */

@Service
class CustomOAuth2UserService(
    private val socialAuthHelper: SocialAuthHelper
) : DefaultOAuth2UserService() {

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
            ?: throw AuthException(
                AuthErrorCode.OAUTH_DATA_ACCESS_FAIL,
                "[CustomOAuth2UserService#loadUser] Email not found from provider: ${provider}",
                "소셜 계정에서 이메일 정보를 불러올 수 없습니다."
            )

        if (!attributes.oAuth2UserInfo.isEmailVerified()) {
            throw AuthException(
                AuthErrorCode.SOCIAL_LOGIN_FAIL,
                "[CustomOAuth2UserService#loadUser] Unverified email attempt: $email",
                "인증되지 않은 이메일 계정으로는 소셜 로그인을 이용할 수 없습니다."
            )
        }

        val member = socialAuthHelper.findOrCreateMember(attributes, email)

        return SecurityUser(
            id = member.id ?: throw IllegalStateException("회원 가입에 실패했습니다."),
            email = member.email,
            password = "",
            nickname = member.nickname,
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes = attributes.attributes
        )
    }
}
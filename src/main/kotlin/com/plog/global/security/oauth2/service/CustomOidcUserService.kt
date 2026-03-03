package com.plog.global.security.oauth2.service

import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.security.SecurityUser
import com.plog.global.security.oauth2.util.SocialAuthHelper
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service

/**
 * OpenID Connect(OIDC) 프로토콜을 사용하는 소셜 로그인의 사용자 정보를 처리하는 서비스입니다.
 * <p>
 * ID Token을 포함한 OIDC 확장 기능을 활용하며,
 * 인증된 사용자 정보를 바탕으로 시스템 내부 회원 엔티티와의 매핑 및 자동 가입을 수행합니다.
 *
 * <p><b>작동 원리:</b><br>
 * 1. {@code super.loadUser}를 통해 OIDC 규격의 유저 정보({@link OidcUser})를 가져옵니다.<br>
 * 2. OIDC 표준 클레임인 {@code email}과 {@code subject}(ProviderId)를 추출합니다.<br>
 * 3. {@link SocialAuthHelper}를 사용하여 기존 가입 여부를 확인하고, 필요 시 회원가입 또는 계정 연동을 진행합니다.<br>
 * 4. 최종적으로 OIDC 전용 데이터(ID Token, UserInfo)를 포함한 {@link SecurityUser}를 반환하여 세션을 형성합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link OidcUserService}를 상속받아 OIDC 유저 로딩 로직을 커스터마이징합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code CustomOidcUserService(SocialAuthHelper socialAuthHelper)}<br>
 * 회원 조회 및 가입 처리를 위임할 {@code socialAuthHelper}를 주입받습니다.
 *
 * <p><b>빈 관리:</b><br>
 * {@code @Service}로 선언되어 스프링 컨테이너에 의해 싱글톤 빈으로 관리됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Spring Security OAuth2 Client의 OIDC 지원 모듈을 사용합니다.
 *
 * @author minhee
 * @since 2026-03-02
 * @see OidcUser
 * @see SocialAuthHelper
 * @see SecurityUser
 */

@Service
class CustomOidcUserService(
    private val socialAuthHelper: SocialAuthHelper
) : OidcUserService() {

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = super.loadUser(userRequest)

        val email = oidcUser.email
            ?: throw AuthException(
                AuthErrorCode.OAUTH_DATA_ACCESS_FAIL,
                "[CustomOidcUserService#loadUser] OIDC email claim is missing for provider: ${userRequest.clientRegistration.registrationId}",
                "소셜 계정에서 이메일 정보를 불러올 수 없습니다."
            )

        val isEmailVerified = oidcUser.getClaimAsBoolean("email_verified") ?: false
        if (!isEmailVerified) {
            throw AuthException(
                AuthErrorCode.SOCIAL_LOGIN_FAIL,
                "[CustomOidcUserService#loadUser] Unverified OIDC email attempt: $email",
                "인증되지 않은 이메일 계정으로는 소셜 로그인을 이용할 수 없습니다."
            )
        }

        val providerId = oidcUser.subject
        val provider = SocialAuthProvider.GOOGLE

        val member = socialAuthHelper.findOrCreateMember(email, provider, providerId)

        return SecurityUser(
            id = member.id!!,
            email = member.email,
            nickname = member.nickname,
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes = oidcUser.attributes,
            idToken = oidcUser.idToken,
            oidcUserInfo = oidcUser.userInfo
        )
    }
}
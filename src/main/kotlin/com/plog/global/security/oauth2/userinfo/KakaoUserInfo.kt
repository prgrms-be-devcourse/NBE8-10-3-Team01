package com.plog.global.security.oauth2.userinfo

import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException


/**
 * 카카오(Kakao) 소셜 공급자로부터 받은 사용자 정보를 파싱하여 서비스 표준 규격으로 변환하는 클래스입니다.
 *
 * <p>카카오 프로필 API 응답의 중첩된 JSON 구조에서 필요한 사용자 속성을 추출합니다.
 *
 * <p><b>상속 정보:</b><br>
 * [OAuth2UserInfo] 인터페이스를 구현합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code KakaoUserInfo(Map<String, Any> attributes)}<br>
 * 카카오 리소스 서버로부터 수신한 원본 사용자 속성 맵을 주입받아 초기화합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 카카오 프로필 API(OAuth 2.0) 규격을 기반으로 동작합니다.
 *
 * @author minhee
 * @since 2026-02-25
 * @see OAuth2UserInfo
 * @see com.plog.global.security.oauth2.OAuth2Attributes
 */

class KakaoUserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    private val kakaoAccount = attributes["kakao_account"] as? Map<*, *>
    private val kakaoProfile = kakaoAccount?.get("profile") as? Map<*, *>

    override fun getProvider(): SocialAuthProvider = SocialAuthProvider.KAKAO

    override fun getProviderId(): String = attributes["id"]?.toString()
        ?: throw AuthException(
            AuthErrorCode.OAUTH_DATA_ACCESS_FAIL,
            "[KakaoUserInfo#getProviderId] can't find provider id from attributes: $attributes",
            "카카오 계정 정보를 불러오는 데 실패했습니다."
        )

    override fun getEmail(): String? = kakaoAccount?.get("email") as? String
    override fun getNickname(): String? = kakaoProfile?.get("nickname") as? String
    override fun isEmailVerified(): Boolean = kakaoAccount?.get("is_email_verified") as? Boolean ?: false
}
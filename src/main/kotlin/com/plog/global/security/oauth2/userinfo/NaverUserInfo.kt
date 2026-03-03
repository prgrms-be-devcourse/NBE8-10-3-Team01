package com.plog.global.security.oauth2.userinfo

import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException


/**
 * 네이버(Naver) 소셜 공급자로부터 받은 유저 데이터를 파싱하는 구현체입니다.
 *
 * 네이버 프로필 API 응답 내 'response' 맵에서 필요한 정보를 추출하며,
 * 네이버의 보안 정책(실명 인증 계정 정보 제공)을 준수하여 데이터를 처리합니다.
 *
 * <p><b>상속 정보:</b><br>
 * [OAuth2UserInfo] 인터페이스를 구현합니다.
 *
 * @author minhee
 * @since 2026-02-25
 * @see
 */

class NaverUserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    private val response = attributes["response"] as? Map<*, *>

    override fun getProvider(): SocialAuthProvider = SocialAuthProvider.NAVER
    override fun getProviderId(): String = response?.get("id") as? String
        ?: throw AuthException(
            AuthErrorCode.OAUTH_DATA_ACCESS_FAIL,
            "[NaverUserInfo#getProviderId] can't find provider id from response: $response",
            "네이버 계정 정보를 불러오는 데 실패했습니다."
        )
    override fun getEmail(): String? = response?.get("email") as? String
    override fun getNickname(): String? = response?.get("name") as? String
    override fun isEmailVerified(): Boolean = getEmail() != null
}
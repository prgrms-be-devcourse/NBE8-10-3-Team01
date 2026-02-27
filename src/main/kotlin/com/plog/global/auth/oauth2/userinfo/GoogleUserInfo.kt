package com.plog.global.auth.oauth2.userinfo

import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException


/**
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
 * @since 2026-02-25
 * @see
 */

class GoogleUserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override fun getProvider(): SocialAuthProvider = SocialAuthProvider.GOOGLE

    override fun getProviderId(): String = attributes["sub"] as? String
        ?: throw AuthException(
            AuthErrorCode.OAUTH_DATA_ACCESS_FAIL,
            "[GoogleUserInfo#getProviderId] can't find provider id (sub) from attributes: $attributes",
            "구글 계정 정보를 불러오는 데 실패했습니다."
        )

    override fun getEmail(): String? = attributes["email"] as? String
    override fun getNickname(): String? = attributes["name"] as? String
}
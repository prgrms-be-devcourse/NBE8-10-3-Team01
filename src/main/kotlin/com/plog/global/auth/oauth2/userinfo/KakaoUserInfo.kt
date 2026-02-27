package com.plog.global.auth.oauth2.userinfo

import com.plog.domain.member.entity.SocialAuthProvider


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

class KakaoUserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {
    private val kakaoAccount = attributes["kakao_account"] as? Map<*, *>
    private val kakaoProfile = kakaoAccount?.get("profile") as? Map<*, *>

    override fun getProvider(): SocialAuthProvider = SocialAuthProvider.KAKAO
    override fun getProviderId(): String = attributes["id"].toString()
    override fun getEmail(): String = kakaoAccount?.get("email").toString()
    override fun getNickname(): String = kakaoProfile?.get("nickname").toString()
}
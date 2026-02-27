package com.plog.global.security.oauth2

import com.plog.domain.member.entity.SocialAuthProvider
import com.plog.global.security.oauth2.userinfo.GoogleUserInfo
import com.plog.global.security.oauth2.userinfo.KakaoUserInfo
import com.plog.global.security.oauth2.userinfo.NaverUserInfo
import com.plog.global.security.oauth2.userinfo.OAuth2UserInfo


/**
 * 각 소셜 서비스(Google, Kakao, Naver 등)로부터 전달받은 유저 속성(Attributes)을
 * 서비스 표준 규격으로 통합 관리하는 DTO 클래스입니다.
 * <p><b>작동 원리:</b><br>
 * 각 공급자마다 다른 JSON 응답 구조를 {@link OAuth2UserInfo} 인터페이스의 구현체로 매핑합니다.
 * 정적 팩토리 메서드인 {@code of}를 통해 공급자별 분기 처리를 수행하며,
 * 이를 통해 서비스 로직(CustomOAuth2UserService)이 공급자 종류에 의존하지 않게 합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 별도의 상속 관계는 없으나, {@link OAuth2UserInfo} 구현체를 포함(Composition)하여
 * 데이터를 가공합니다.
 *
 * <p><b>주요 필드:</b><br>
 * {@code attributes}: 소셜 서버에서 전달받은 원본 Map 데이터 <br>
 * {@code nameAttributeKey}: OAuth2 로그인 시 기본 식별자로 사용되는 키값 (id, sub 등) <br>
 * {@code oAuth2UserInfo}: 가공된 유저 정보 데이터 추출기
 *
 * <p><b>외부 모듈:</b><br>
 * Spring Security OAuth2 Client 모듈의 속성 데이터를 입력받습니다.
 *
 * @author minhee
 * @since 2026-02-25
 */

class OAuth2Attributes(
    val attributes: Map<String, Any>,
    val nameAttributeKey: String,
    val oAuth2UserInfo: OAuth2UserInfo
) {
    companion object {
        /**
         * 공급자(Provider) 타입에 따라 적절한 OAuth2Attributes 객체를 생성하여 반환합니다.
         *
         * @param provider 소셜 로그인 공급자 (Enum)
         * @param userNameAttributeName OAuth2 로그인 시 주 식별자가 되는 키 이름
         * @param attributes 소셜 서버로부터 받은 원본 데이터 맵
         * @return 각 소셜 로직이 반영된 OAuth2Attributes 객체
         */
        fun of(
            provider: SocialAuthProvider,
            userNameAttributeName: String,
            attributes: Map<String, Any>
        ): OAuth2Attributes {
            return when(provider) {
                SocialAuthProvider.KAKAO -> ofKakao(userNameAttributeName, attributes)
                SocialAuthProvider.GOOGLE -> ofGoogle(userNameAttributeName, attributes)
                SocialAuthProvider.NAVER -> ofNaver(userNameAttributeName, attributes)
            }
        }

        private fun ofKakao(userNameAttributeName: String, attributes: Map<String, Any>): OAuth2Attributes {
            return OAuth2Attributes(
                attributes = attributes,
                nameAttributeKey = userNameAttributeName,
                oAuth2UserInfo = KakaoUserInfo(attributes)
            )
        }

        private fun ofGoogle(userNameAttributeName: String, attributes: Map<String, Any>): OAuth2Attributes {
            return OAuth2Attributes(
                attributes = attributes,
                nameAttributeKey = userNameAttributeName,
                oAuth2UserInfo = GoogleUserInfo(attributes)
            )
        }

        private fun ofNaver(userNameAttributeName: String, attributes: Map<String, Any>): OAuth2Attributes {
            return OAuth2Attributes(
                attributes = attributes,
                nameAttributeKey = userNameAttributeName,
                oAuth2UserInfo = NaverUserInfo(attributes)
            )
        }
    }

    // 편의성을 위해 추가 attributes.oAuth2UserInfo.getEmail() -> attributes.getEmail()
    fun getProviderId(): String = oAuth2UserInfo.getProviderId()
    fun getEmail(): String? = oAuth2UserInfo.getEmail()
    fun getName(): String? = oAuth2UserInfo.getNickname()
    fun getProvider(): SocialAuthProvider = oAuth2UserInfo.getProvider()
}
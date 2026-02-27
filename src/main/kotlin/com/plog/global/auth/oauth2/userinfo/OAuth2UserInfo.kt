package com.plog.global.auth.oauth2.userinfo

import com.plog.domain.member.entity.SocialAuthProvider


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
 * @since 2026-02-25
 * @see
 */

interface OAuth2UserInfo {
    fun getProvider(): SocialAuthProvider
    fun getProviderId(): String
    fun getEmail(): String?    // 서비스 상 필수지만 없을 수도 있으므로 nullable -> Service에서 처리
    fun getNickname(): String? // 당장 사용하지는 않지만 제공받는 정보로서 정의
}
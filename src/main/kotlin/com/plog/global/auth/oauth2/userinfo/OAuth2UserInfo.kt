package com.plog.global.auth.oauth2.userinfo

import com.plog.domain.member.entity.SocialAuthProvider


/**
 * 소셜 공급자(Kakao, Naver, Google 등)로부터 받은 원본 데이터를
 * 우리 서비스 표준 규격으로 추출하기 위한 공통 인터페이스입니다.
 *
 * @author minhee
 * @since 2026-02-25
 */

interface OAuth2UserInfo {
    /**
     * 현재 로그인을 시도한 소셜 공급자 타입을 반환합니다.
     */
    fun getProvider(): SocialAuthProvider

    /**
     * 소셜 공급자에서 제공하는 고유 식별자(ID)를 반환합니다.
     * 시스템 식별자이므로 반드시 존재해야 하며, 없을 경우 AuthException을 던집니다.
     */
    fun getProviderId(): String

    /**
     * 소셜 계정에 등록된 이메일 정보를 반환합니다.
     * 공급자 설정이나 유저 선택에 따라 null일 수 있으므로 서비스 레이어에서 후처리합니다.
     */
    fun getEmail(): String?    // 서비스 상 필수지만 없을 수도 있으므로 nullable -> Service에서 처리

    /**
     * 소셜 계정에 등록된 닉네임 정보를 반환합니다.
     */
    fun getNickname(): String? // 당장 사용하지는 않지만 제공받는 정보로서 정의
}
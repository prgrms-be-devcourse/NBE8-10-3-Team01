package com.plog.domain.member.entity

/**
 * 소셜 로그인 공급자(Provider)를 관리하는 Enum 클래스입니다.
 *
 * <p><b>주요 기능:</b><br>
 * 지원하는 소셜 서비스(카카오, 네이버, 구글)를 구분하며,
 * 엔티티에서는 문자열 형태로 저장됩니다.
 *
 * @author minhee
 * @since 2026-02-25
 */
enum class SocialAuthProvider() {
    KAKAO,
    NAVER,
    GOOGLE
}
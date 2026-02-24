// src/main/kotlin/com/plog/domain/member/dto/AuthLoginResult.kt
package com.plog.domain.member.dto

/**
 * 로그인과 토큰 재발급 시 서비스에서 컨트롤러에게 전달하는 데이터입니다.
 *
 * accessToken은 헤더 설정에, refreshToken은 쿠키 설정에 사용됩니다.
 *
 * @property id 인증된 사용자의 ID
 * @property nickname 인증된 사용자의 닉네임
 * @property accessToken 이후 요청에 사용될 Bearer 인증 토큰
 * @property refreshToken 쿠키에 새로 저장될 토큰
 */
data class AuthLoginResult(
    val id: Long,
    val nickname: String,
    val accessToken: String,
    val refreshToken: String
)
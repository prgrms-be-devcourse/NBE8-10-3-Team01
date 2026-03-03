// src/main/kotlin/com/plog/domain/member/dto/AuthInfoRes.kt
package com.plog.domain.member.dto

/**
 * 인증 성공 시 사용자에게 반환되는 데이터 구조입니다.
 *
 * Access Token을 응답 헤더뿐만 아니라 바디에 중복 포함하여,
 * 프론트엔드 환경에서 토큰 추출 및 상태 관리를 용이하게 돕습니다.
 *
 * @property id 인증된 사용자의 ID
 * @property nickname 인증된 사용자의 닉네임
 * @property accessToken 이후 요청에 사용될 Bearer 인증 토큰
 */
data class AuthInfoRes(
    val id: Long = 0L,
    val nickname: String = "",
    val accessToken: String = ""
) {
    companion object {
        @JvmStatic
        fun from(res: AuthLoginResult): AuthInfoRes {
            return AuthInfoRes(
                id = res.id,
                nickname = res.nickname,
                accessToken = res.accessToken
            )
        }
    }
}

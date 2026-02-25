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
    val id: Long,
    val nickname: String,
    val accessToken: String
) {
    companion object {
        @JvmStatic // TODO: 전체 마이그레이션 후 전체 Dto에서 해당 어노테이션 제거
        fun from(res: AuthLoginResult): AuthInfoRes {
            return AuthInfoRes(
                id = res.id,
                nickname = res.nickname,
                accessToken = res.accessToken
            )
        }
        @JvmStatic
        fun builder() = AuthInfoResBuilder()
    }

    // TODO: 전체 마이그레이션 완료 후 삭제
    class AuthInfoResBuilder {
        private var id: Long = 0L
        private var nickname: String = ""
        private var accessToken: String = ""

        fun id(id: Long) = apply { this.id = id }
        fun nickname(nickname: String) = apply { this.nickname = nickname }
        fun accessToken(accessToken: String) = apply { this.accessToken = accessToken }

        fun build() = AuthInfoRes(
            id = id,
            nickname = nickname,
            accessToken = accessToken
        )
    }
}
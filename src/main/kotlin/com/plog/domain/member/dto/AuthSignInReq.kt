// src/main/kotlin/com/plog/domain/member/dto/AuthSignInReq.kt
package com.plog.domain.member.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * 로그인 요청을 위한 데이터 구조입니다.
 *
 * @property email 가입된 이메일 주소
 * @property password 계정 비밀번호
 */
data class AuthSignInReq(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)
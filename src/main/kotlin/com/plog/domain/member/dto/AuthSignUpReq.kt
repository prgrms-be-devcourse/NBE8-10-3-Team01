// src/main/kotlin/com/plog/domain/member/dto/AuthSignUpReq.kt
package com.plog.domain.member.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * 회원가입 요청을 위한 데이터 구조입니다.
 *
 * @property email 가입하고자 하는 사용자의 이메일 주소 (ID 역할)
 * @property password 사용자의 비밀번호
 * @property nickname 서비스 내에서 사용할 사용자의 별명
 */
data class AuthSignUpReq(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String,

    @field:NotBlank
    val nickname: String
)
// src/main/kotlin/com/plog/domain/member/dto/MemberInfoRes.kt
package com.plog.domain.member.dto

import com.plog.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * member 데이터를 조회할 때, 반환되는 데이터의 기본 형식입니다. 사용자의 기본적인 데이터가 포함되어 있습니다.
 *
 * @author jack8
 * @since 2026-01-18
 */
data class MemberInfoRes(
    val id: Long = 0L,
    val email: String = "",
    val nickname: String = "",
    val profileImageUrl: String? = null,
    val createDate: LocalDateTime? = null
) {
    companion object {
        @JvmStatic
        fun from(member: Member): MemberInfoRes {
            return MemberInfoRes(
                id = member.id ?: 0L,
                email = member.email,
                nickname = member.nickname,
                createDate = member.createDate,
                profileImageUrl = member.profileImage?.accessUrl
            )
        }
    }
}

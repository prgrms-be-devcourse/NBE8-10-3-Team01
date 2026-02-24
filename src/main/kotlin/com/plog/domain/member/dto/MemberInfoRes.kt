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
    val id: Long,
    val email: String,
    val nickname: String,
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
        @JvmStatic
        fun builder() = MemberInfoResBuilder()
    }

    // TODO: 전체 마이그레이션 완료 후 삭제
    class MemberInfoResBuilder {
        private var id: Long = 0L
        private var email: String = ""
        private var nickname: String = ""
        private var profileImageUrl: String? = null
        private var createDate: LocalDateTime = LocalDateTime.now()

        fun id(id: Long) = apply { this.id = id }
        fun email(email: String) = apply { this.email = email }
        fun nickname(nickname: String) = apply { this.nickname = nickname }
        fun profileImageUrl(profileImageUrl: String?) = apply { this.profileImageUrl = profileImageUrl }
        fun createDate(createDate: LocalDateTime) = apply { this.createDate = createDate }

        fun build() = MemberInfoRes(
            id = id,
            email = email,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            createDate = createDate
        )
    }
}
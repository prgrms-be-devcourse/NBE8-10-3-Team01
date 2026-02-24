package com.plog.domain.image.dto

import com.plog.domain.member.entity.Member

/**
 * 프로필 이미지 업로드/조회 작업 완료 후 클라이언트에게 반환되는 응답 DTO입니다.
 *
 * @param memberId 회원 고유 ID
 * @param profileImageUrl 프로필 이미지 URL (http://...)
 * @author Jaewon Ryu
 * @since 2026-01-23
 */

data class ProfileImageUploadRes(
    val memberId: Long?,
    val profileImageUrl: String?
) {
    companion object {
        @JvmStatic
        fun from(member: Member): ProfileImageUploadRes {
            val imageUrl = member.profileImage?.accessUrl
            return ProfileImageUploadRes(member.id, imageUrl)
        }
    }
}
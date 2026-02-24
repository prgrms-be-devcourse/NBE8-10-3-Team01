// src/main/kotlin/com/plog/domain/member/entity/Member.kt
package com.plog.domain.member.entity

import com.plog.domain.image.entity.Image
import com.plog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Member(
    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(unique = true, nullable = false)
    var nickname: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id")
    var profileImage: Image? = null
) : BaseEntity() {

    // TODO: 전체 마이그레이션 완료 후 삭제
    companion object {
        @JvmStatic
        fun builder() = MemberBuilder()
    }

    class MemberBuilder {
        private var email: String = ""
        private var password: String = ""
        private var nickname: String = ""
        private var profileImage: Image? = null

        fun email(email: String) = apply { this.email = email }
        fun password(password: String) = apply { this.password = password }
        fun nickname(nickname: String) = apply { this.nickname = nickname }
        fun profileImage(profileImage: Image?) = apply { this.profileImage = profileImage }

        fun build() = Member(
            email = email,
            password = password,
            nickname = nickname,
            profileImage = profileImage
        )
    }

    /**
     * 엔티티의 update 메서드입니다.
     *
     * 해당 메서드를 이런 방식으로 구현한 까닭은 다음과 같습니다.
     * 1. update 가 필요한 필드를 setter 로 두는 것보다, 도메인 상 기능이 명확한 메서드로 묶는 편이 좋다고 봤습니다.
     * 2. entity 는 dto 의 존재를 좋지 않다고 생각합니다. DTO는 entity 의 데이터를 가공하여 운송하는 책임을
     *    가지고 있지만, entity 에서 dto의 존재를 아는 순간 둘 사이 간의 결합이 발생합니다.
     *
     * @param nickname 변경할 파라미터 중 닉네임
     * @return 변경된 엔티티
     */
    fun update(nickname: String): Member {
        this.nickname = nickname
        return this
    }

    fun updateProfileImage(profileImage: Image?) {
        this.profileImage = profileImage
    }
}
package com.plog.domain.member.entity

import com.plog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * 소셜 로그인 인증 정보를 관리하는 엔티티입니다.
 * 특정 회원이 어떤 소셜 서비스(Google, Kakao 등)를 통해
 * 어떤 고유 식별값으로 연결되어 있는지 저장합니다.
 *
 * @author minhee
 * @since 2026-02-25
 */

@Entity
@Table(
    name = "social_auth",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_social_auth_provider_provider_id",
            columnNames = ["provider", "provider_id"]
        ),
        UniqueConstraint(
            name = "uk_social_auth_member_id_provider",
            columnNames = ["member_id", "provider"]
        )
    ]
)
class SocialAuth(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    val member: Member,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val provider: SocialAuthProvider,

    @Column(name="provider_id", nullable = false, updatable = false)
    val providerId: String
) : BaseEntity()
package com.plog.domain.member.dto;

import com.plog.domain.member.entity.Member;

import java.time.LocalDateTime;

/**
 * member 데이터를 조회할 때, 반환되는 데이터의 기본 형식입니다. 사용자의 기본적인 데이터가 포함되어 있습니다.
 *
 * @author jack8
 * @since 2026-01-18
 */
public record MemberInfoRes(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        LocalDateTime createDate
) {

    public static MemberInfoResBuilder builder() {
        return new MemberInfoResBuilder();
    }

    public static class MemberInfoResBuilder {
        private Long id;
        private String email;
        private String nickname;
        private String profileImageUrl;
        private LocalDateTime createDate;

        MemberInfoResBuilder() {
        }

        public MemberInfoResBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MemberInfoResBuilder email(String email) {
            this.email = email;
            return this;
        }

        public MemberInfoResBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public MemberInfoResBuilder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public MemberInfoResBuilder createDate(LocalDateTime createDate) {
            this.createDate = createDate;
            return this;
        }

        public MemberInfoRes build() {
            return new MemberInfoRes(id, email, nickname, profileImageUrl, createDate);
        }
    }

    public static MemberInfoRes from(Member member) {
        return MemberInfoRes.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .createDate(member.getCreateDate())
                .profileImageUrl(member.getProfileImage() != null ? member.getProfileImage().getAccessUrl() : null)
                .build();
    }
}
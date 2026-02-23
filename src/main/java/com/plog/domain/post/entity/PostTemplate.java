package com.plog.domain.post.entity;

import com.plog.domain.member.entity.Member;
import com.plog.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * 게시글 작성 시 게시글의 템플릿을 기존에 설정해 둘 수 있습니다. 이를 위한 엔티티 클래스입니다.
 * <p>
 * 제목, 본문, author 에 대한 데이터를 가지고 있습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseEntity}를 상속받아 고유 식별자(id)와 생성/수정 시간을 공통으로 관리합니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
@Entity
public class PostTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected PostTemplate() {
    }

    public PostTemplate(String name, String title, String content, Member member) {
        this.name = name;
        this.title = title;
        this.content = content;
        this.member = member;
    }

    public static PostTemplateBuilder builder() {
        return new PostTemplateBuilder();
    }

    public static class PostTemplateBuilder {
        private String name;
        private String title;
        private String content;
        private Member member;

        PostTemplateBuilder() {
        }

        public PostTemplateBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PostTemplateBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostTemplateBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostTemplateBuilder member(Member member) {
            this.member = member;
            return this;
        }

        public PostTemplate build() {
            return new PostTemplate(name, title, content, member);
        }
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Member getMember() {
        return member;
    }

    public PostTemplate update(String name, String title, String content) {
        this.title = title;
        this.content = content;
        this.name = name;

        return this;
    }
}

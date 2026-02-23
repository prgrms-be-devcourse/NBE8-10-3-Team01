package com.plog.domain.post.entity;

import com.plog.domain.hashtag.entity.PostHashTag;
import com.plog.domain.member.entity.Member;
import com.plog.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 블로그 게시물의 핵심 데이터를 담당하는 엔티티 클래스입니다.
 * <p>
 * 마크다운 형식의 본문과 검색 최적화를 위한 순수 텍스트, 요약글 등을 관리하며,
 * JPA를 통해 MySQL의 MEDIUMTEXT 타입과 매핑되어 대용량 텍스트를 저장합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseEntity}를 상속받아 고유 식별자(id)와 생성/수정 시간을 공통으로 관리합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * JPA 프록시 생성을 위해 {@code protected} 수준의 기본 생성자가 포함되어 있습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Jakarta Persistence API를 사용합니다.
 *
 * @author MintyU
 * @since 2026-01-15
 * @see BaseEntity
 */

@Entity
public class Post extends BaseEntity{
    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Column(length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.DRAFT;

    private int viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostHashTag> postHashTags = new ArrayList<>();

    private String thumbnail;

    protected Post() {
    }

    public Post(String title, String content, String summary, PostStatus status, int viewCount, Member member, List<PostHashTag> postHashTags, String thumbnail) {
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.status = status;
        this.viewCount = viewCount;
        this.member = member;
        this.postHashTags = postHashTags;
        this.thumbnail = thumbnail;
    }

    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public static class PostBuilder {
        private String title;
        private String content;
        private String summary;
        private PostStatus status = PostStatus.DRAFT;
        private int viewCount = 0;
        private Member member;
        private List<PostHashTag> postHashTags = new ArrayList<>();
        private String thumbnail;

        PostBuilder() {
        }

        public PostBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public PostBuilder status(PostStatus status) {
            this.status = status;
            return this;
        }

        public PostBuilder viewCount(int viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public PostBuilder member(Member member) {
            this.member = member;
            return this;
        }

        public PostBuilder postHashTags(List<PostHashTag> postHashTags) {
            this.postHashTags = postHashTags;
            return this;
        }

        public PostBuilder thumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
            return this;
        }

        public Post build() {
            return new Post(title, content, summary, status, viewCount, member, postHashTags, thumbnail);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public PostStatus getStatus() {
        return status;
    }

    public int getViewCount() {
        return viewCount;
    }

    public Member getMember() {
        return member;
    }

    public List<PostHashTag> getPostHashTags() {
        return postHashTags;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void update(String title, String content, String summary, String thumbnail) {
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.thumbnail = thumbnail;
    }

}

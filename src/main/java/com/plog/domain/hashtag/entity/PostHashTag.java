package com.plog.domain.hashtag.entity;

import com.plog.domain.post.entity.Post;
import com.plog.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "post_hashtag")
public class PostHashTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private HashTag hashTag;

    private String displayName;

    protected PostHashTag() {
    }

    public PostHashTag(Post post, HashTag hashTag, String displayName) {
        this.displayName = displayName;
        this.post = post;
        this.hashTag = hashTag;
    }

    public static PostHashTagBuilder builder() {
        return new PostHashTagBuilder();
    }

    public static class PostHashTagBuilder {
        private Post post;
        private HashTag hashTag;
        private String displayName;

        PostHashTagBuilder() {
        }

        public PostHashTagBuilder post(Post post) {
            this.post = post;
            return this;
        }

        public PostHashTagBuilder hashTag(HashTag hashTag) {
            this.hashTag = hashTag;
            return this;
        }

        public PostHashTagBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public PostHashTag build() {
            return new PostHashTag(post, hashTag, displayName);
        }
    }

    public Post getPost() {
        return post;
    }

    public HashTag getHashTag() {
        return hashTag;
    }

    public String getDisplayName() {
        return displayName;
    }
}
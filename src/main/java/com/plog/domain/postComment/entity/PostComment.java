package com.plog.domain.postComment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * @author njwwn
 * @see
 * @since 2026-01-15
 */
@Getter
@Setter
@Entity
public class PostComment extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Memeber author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PostComment parent;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private boolean deleted = false;

    public PostComment(Member author, Post post, String content){
        this.author = author;
        this.post = post;
        this.content = content;
    }

    public void modify(String content){
        this.content = content;
    }


}

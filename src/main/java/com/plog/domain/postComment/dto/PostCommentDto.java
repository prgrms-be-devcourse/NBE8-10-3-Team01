package com.plog.domain.postComment.dto;

import com.plog.domain.postComment.entity.PostComment;

import java.time.LocalDateTime;

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
public record PostCommentDto(
        int id,
        String content,
        int authorId,
        int postId,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    public PostCommentDto(PostComment postComment){
        this(
                postComment.getId(),
                postComment.getContent(),
                postComment.getAuthor().getId(),
                postComment.getPost().getId(),
                postComment.getCreated_date(),
                postComment.getModified_date()
        );
    }

}

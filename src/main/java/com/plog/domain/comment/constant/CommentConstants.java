package com.plog.domain.comment.constant;

/**
 * 댓글 도메인에 사용되는 수를 상수화하는 클래스입니다.
 * <p>
 *
 * @author njwwn
 * @since 2026-01-21
 */
public class CommentConstants {

    private CommentConstants() {}

    // 댓글 페이징 사이즈
    public static final int COMMENT_PAGE_SIZE = 10;

    // 대댓글 페이징 사이즈
    public static final int REPLY_PAGE_SIZE = 5;

    // 기본 정렬 기준 필드
    public static final String DEFAULT_SORT_FIELD = "createDate";
}

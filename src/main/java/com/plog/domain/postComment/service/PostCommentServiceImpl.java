package com.plog.domain.postComment.service;

import com.plog.domain.post.entity.Post;
import com.plog.domain.post.repository.PostRepository;
import com.plog.domain.postComment.dto.GetPostCommentRes;
import com.plog.domain.postComment.entity.PostComment;
import com.plog.domain.postComment.repository.PostCommentRepository;
import com.plog.global.exception.errorCode.PostCommentErrorCode;
import com.plog.global.exception.exceptions.PostCommentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCommentServiceImpl implements PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;


    @Override
    @Transactional
    public Long createComment(Long postId, String content, Long parentCommentId){

        //TODO: 추후 Post 예외처리 정책으로 수정 예정.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        PostComment parentComment = null;

        if(parentCommentId != null){
            parentComment = postCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new PostCommentException(
                            PostCommentErrorCode.COMMENT_NOT_FOUND,
                            "[PostCommentService#createComment] parent comment not found. parentCommentId=" + parentCommentId,
                            "부모 댓글이 존재하지 않습니다."
                    ));
        }

        PostComment comment = PostComment.builder()
                .post(post)
                .content(content)
                .parent(parentComment)
                .build();

        return postCommentRepository.save(comment).getId();
    }

    @Override
    public List<GetPostCommentRes> getCommentsByPostId(Long postId, int page) {

        int pageSize = 10;

        // TODO: 추후 postService.findById(postId)로 교체
        // 현재는 postId만 활용하여 Repository 조회 가능

        Page<PostComment> commentPage = postCommentRepository.findAllByPostId(
                postId,
                PageRequest.of(page, pageSize, Sort.by("createDate").descending()) // 최신 댓글 먼저
        );

        return commentPage.stream()
                .map(GetPostCommentRes::new)
                .toList();
    }


    // 엔티티 노출 위험에 있어서 리턴 타입을 UpdateCommentRes으로 바꿀 필요가 있다.
    @Override
    public PostComment updateComment(Long commentId, String content) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new PostCommentException(
                        PostCommentErrorCode.COMMENT_NOT_FOUND,
                        "[PostCommentService#deleteComment] can't find comment by id : " + commentId,
                        "존재하지 않는 댓글입니다."
                ));

        comment.modify(content);

        return comment;
    }


    @Override
    public void deleteComment(Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new PostCommentException(
                        PostCommentErrorCode.COMMENT_NOT_FOUND,
                        "[PostCommentService#deleteComment] can't find comment by id : " + commentId,
                        "존재하지 않는 댓글입니다."
                ));

        //멱등성의 의식한 로직
        if(comment.isDeleted()){
            return;
        }
        comment.delete();
    }
}

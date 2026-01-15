package com.plog.domain.postComment.service;

import com.plog.domain.postComment.repository.PostCommentRepository;
import com.plog.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;

    public PostComment write(Member author, PostComment postComment){
        return postCommentRepository.save(postComment);
    }

    public void modify(PostComment postComment, String content){
        postComment.modify(content);
    }

    public void delete(PostComment postComment){
        postCommentRepository.delete(postComment);
    }


}

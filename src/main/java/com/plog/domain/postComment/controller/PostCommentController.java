package com.plog.domain.postComment.controller;

import com.plog.domain.postComment.dto.PostCommentDto;
import com.plog.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

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

@RestController
@Validated
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostService postservice;

    record PostCommentWriteReqBody(
            String content
    ) {
    }

    @PostMapping
    @Transactional
    public CommonResponse<PostCommentDto> write(
    )

    public List<PostCommentDto> getItems(
            @PathVariable int postId
    ){
        Post post = postService.findById(postId).get();

        //수정 필요
        return post
                .getComments()
                .stream()
                .map(PostCommentDto::new)
                .toList();
    }
}

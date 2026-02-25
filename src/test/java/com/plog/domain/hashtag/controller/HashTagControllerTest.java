package com.plog.domain.hashtag.controller;

import com.plog.domain.hashtag.service.HashTagService;
import com.plog.domain.post.dto.PostListRes;
import com.plog.testUtil.SecurityTestConfig;
import com.plog.testUtil.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HashTagController.class)
@Import({SecurityTestConfig.class})
@ActiveProfiles("test")
class HashTagControllerTest extends WebMvcTestSupport {

    @MockitoBean
    private HashTagService hashTagService;

    @Test
    @DisplayName("해시태그 검색 API 성공")
    void searchHashTagSuccess() throws Exception {
        // [Given]
        String keyword = "java";
        Pageable pageable = PageRequest.of(0, 10);
        PostListRes res = new PostListRes(
                1L, "제목", "요약", 0, null, null, List.of("java"), null, "nickname", null
        );
        Page<PostListRes> pageResponse = new PageImpl<>(List.of(res), pageable, 1);

        given(hashTagService.searchPostsByTag(anyString(), any(Pageable.class)))
                .willReturn(pageResponse);

        // [When]
        ResultActions resultActions = mockMvc.perform(get("/api/hashtags/search")
                        .param("keyword", keyword)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        // [Then]
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("해시태그 검색 성공"))
                .andExpect(jsonPath("$.data.content[0].title").value("제목"))
                .andExpect(jsonPath("$.data.content[0].hashtags[0]").value("java"));
    }
}

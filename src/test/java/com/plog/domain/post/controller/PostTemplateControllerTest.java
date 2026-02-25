package com.plog.domain.post.controller;

import com.plog.domain.post.dto.PostTemplateInfoDto;
import com.plog.domain.post.dto.PostTemplateSummaryRes;
import com.plog.domain.post.dto.PostTemplateUpdateReq;
import com.plog.domain.post.service.PostTemplateService;
import com.plog.testUtil.SecurityTestConfig;
import com.plog.testUtil.WebMvcTestSupport;
import com.plog.testUtil.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostTemplateController.class)
@Import({SecurityTestConfig.class})
@ActiveProfiles("test")
class PostTemplateControllerTest extends WebMvcTestSupport {

    @MockitoBean
    private PostTemplateService postTemplateService;

    @Test
    @DisplayName("포스트 템플릿 생성 API 성공")
    @WithCustomMockUser
    void createPostTemplateApiSuccess() throws Exception {
        // [Given]
        Long createdTemplateId = 100L;
        PostTemplateInfoDto request = new PostTemplateInfoDto(null, "템플릿이름", "템플릿제목", "템플릿내용");

        given(postTemplateService.createPostTemplate(anyLong(), any(PostTemplateInfoDto.class)))
                .willReturn(createdTemplateId);

        // [When]
        ResultActions resultActions = mockMvc.perform(post("/api/posts/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // [Then]
        resultActions
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/posts/templates/" + createdTemplateId));
    }

    @Test
    @DisplayName("포스트 템플릿 목록 조회 API 성공")
    @WithCustomMockUser
    void getPostTemplatesApiSuccess() throws Exception {
        // [Given]
        PostTemplateSummaryRes res = new PostTemplateSummaryRes("템플릿이름", 100L);

        given(postTemplateService.getTemplateListByMember(anyLong()))
                .willReturn(List.of(res));

        // [When]
        ResultActions resultActions = mockMvc.perform(get("/api/posts/templates"))
                .andDo(print());

        // [Then]
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("템플릿이름"))
                .andExpect(jsonPath("$.data[0].id").value(100));
    }

    @Test
    @DisplayName("포스트 템플릿 상세 조회 API 성공")
    @WithCustomMockUser
    void getPostTemplateApiSuccess() throws Exception {
        // [Given]
        Long templateId = 100L;
        PostTemplateInfoDto res = new PostTemplateInfoDto(templateId, "이름", "제목", "내용");

        given(postTemplateService.getTemplate(anyLong(), anyLong()))
                .willReturn(res);

        // [When]
        ResultActions resultActions = mockMvc.perform(get("/api/posts/templates/{id}", templateId))
                .andDo(print());

        // [Then]
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.name").value("이름"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    @DisplayName("포스트 템플릿 수정 API 성공")
    @WithCustomMockUser
    void updatePostTemplateApiSuccess() throws Exception {
        // [Given]
        Long mockMemberId = 1L;
        Long templateId = 100L;
        PostTemplateUpdateReq request = new PostTemplateUpdateReq("새이름", "새제목", "새내용");

        // [When]
        ResultActions resultActions = mockMvc.perform(put("/api/posts/templates/{id}", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // [Then]
        resultActions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("포스트 템플릿 삭제 API 성공")
    @WithCustomMockUser
    void deletePostTemplateApiSuccess() throws Exception {
        // [Given]
        Long templateId = 100L;

        // [When]
        ResultActions resultActions = mockMvc.perform(delete("/api/posts/templates/{id}", templateId))
                .andDo(print());

        // [Then]
        resultActions.andExpect(status().isNoContent());
    }
}

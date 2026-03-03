package com.plog.domain.post.controller

import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSummaryRes
import com.plog.domain.post.dto.PostTemplateUpdateReq
import com.plog.domain.post.service.PostTemplateService
import com.plog.testUtil.SecurityTestConfig
import com.plog.testUtil.WebMvcTestSupport
import com.plog.testUtil.WithCustomMockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.*
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PostTemplateController::class)
@Import(SecurityTestConfig::class)
@ActiveProfiles("test")
class PostTemplateControllerTest : WebMvcTestSupport() {

    @MockitoBean
    lateinit var postTemplateService: PostTemplateService

    @Test
    @DisplayName("포스트 템플릿 생성 API 성공")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun createPostTemplateApiSuccess() {
        // [Given]
        val createdTemplateId = 100L
        val request = PostTemplateInfoDto(null, "템플릿이름", "템플릿제목", "템플릿내용")

        whenever(postTemplateService.createPostTemplate(any(), any()))
            .thenReturn(createdTemplateId)

        // [When]
        val resultActions = mockMvc.perform(post("/api/posts/templates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/posts/templates/$createdTemplateId"))
    }

    @Test
    @DisplayName("포스트 템플릿 목록 조회 API 성공")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun getPostTemplatesApiSuccess() {
        // [Given]
        val res = PostTemplateSummaryRes("템플릿이름", 100L)

        whenever(postTemplateService.getTemplateListByMember(any()))
            .thenReturn(listOf(res))

        // [When]
        val resultActions = mockMvc.perform(get("/api/posts/templates"))
            .andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].name").value("템플릿이름"))
            .andExpect(jsonPath("$.data[0].id").value(100))
    }

    @Test
    @DisplayName("포스트 템플릿 상세 조회 API 성공")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun getPostTemplateApiSuccess() {
        // [Given]
        val templateId = 100L
        val res = PostTemplateInfoDto(templateId, "이름", "제목", "내용")

        whenever(postTemplateService.getTemplate(any(), any()))
            .thenReturn(res)

        // [When]
        val resultActions = mockMvc.perform(get("/api/posts/templates/{id}", templateId))
            .andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.id").value(100))
            .andExpect(jsonPath("$.data.name").value("이름"))
            .andExpect(jsonPath("$.data.title").value("제목"))
            .andExpect(jsonPath("$.data.content").value("내용"))
    }

    @Test
    @DisplayName("포스트 템플릿 수정 API 성공")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun updatePostTemplateApiSuccess() {
        // [Given]
        val templateId = 100L
        val request = PostTemplateUpdateReq("새이름", "새제목", "새내용")

        // [When]
        val resultActions = mockMvc.perform(put("/api/posts/templates/{id}", templateId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(print())

        // [Then]
        resultActions.andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("포스트 템플릿 삭제 API 성공")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun deletePostTemplateApiSuccess() {
        // [Given]
        val templateId = 100L

        // [When]
        val resultActions = mockMvc.perform(delete("/api/posts/templates/{id}", templateId))
            .andDo(print())

        // [Then]
        resultActions.andExpect(status().isNoContent)
    }
}

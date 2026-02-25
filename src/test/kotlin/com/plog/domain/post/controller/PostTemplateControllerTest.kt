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
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PostTemplateController::class)
@Import(SecurityTestConfig::class)
@ActiveProfiles("test")
class PostTemplateControllerTest : WebMvcTestSupport() {

    @MockitoBean
    lateinit var postTemplateService: PostTemplateService

    @Test
    @DisplayName("템플릿 생성 성공 - 201, Location 반환")
    @WithCustomMockUser
    fun createPostTemplateSuccess() {
        val templateId = 100L
        val req = PostTemplateInfoDto(name = "name", title = "title", content = "content")
        given(postTemplateService.createPostTemplate(1L, req)).willReturn(templateId)

        mockMvc.perform(
            post("/api/posts/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"name","title":"title","content":"content"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/posts/templates/$templateId"))
    }

    @Test
    @DisplayName("템플릿 단건 조회 성공")
    @WithCustomMockUser
    fun getPostTemplateSuccess() {
        val templateId = 10L
        given(
            postTemplateService.getTemplate(
                1L,
                templateId,
            ),
        ).willReturn(PostTemplateInfoDto(id = templateId, name = "n", title = "t", content = "c"))

        mockMvc.perform(get("/api/posts/templates/{id}", templateId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.id").value(templateId))
            .andExpect(jsonPath("$.data.name").value("n"))
            .andExpect(jsonPath("$.message").value("post template retrieved"))
    }

    @Test
    @DisplayName("템플릿 목록 조회 성공")
    @WithCustomMockUser
    fun getPostTemplatesSuccess() {
        given(postTemplateService.getTemplateListByMember(1L)).willReturn(
            listOf(
                PostTemplateSummaryRes(name = "template-a", id = 1L),
                PostTemplateSummaryRes(name = "template-b", id = 2L),
            ),
        )

        mockMvc.perform(get("/api/posts/templates"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].name").value("template-a"))
            .andExpect(jsonPath("$.data[1].id").value(2))
            .andExpect(jsonPath("$.message").value("post template list retrieved"))
    }

    @Test
    @DisplayName("템플릿 수정 성공 - 204")
    @WithCustomMockUser
    fun updatePostTemplateSuccess() {
        val templateId = 3L
        val req = PostTemplateUpdateReq(name = "new-name", title = "new-title", content = "new-content")

        mockMvc.perform(
            put("/api/posts/templates/{id}", templateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"new-name","title":"new-title","content":"new-content"}"""),
        ).andExpect(status().isNoContent)

        verify(postTemplateService).updatePostTemplate(1L, templateId, req)
    }

    @Test
    @DisplayName("템플릿 삭제 성공 - 204")
    @WithCustomMockUser
    fun deletePostTemplateSuccess() {
        val templateId = 9L

        mockMvc.perform(delete("/api/posts/templates/{id}", templateId))
            .andExpect(status().isNoContent)

        verify(postTemplateService).deleteTemplate(1L, templateId)
    }
}

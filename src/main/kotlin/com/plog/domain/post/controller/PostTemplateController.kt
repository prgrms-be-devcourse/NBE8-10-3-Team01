package com.plog.domain.post.controller

import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSummaryRes
import com.plog.domain.post.dto.PostTemplateUpdateReq
import com.plog.domain.post.service.PostTemplateService
import com.plog.global.response.CommonResponse
import com.plog.global.response.Response
import com.plog.global.security.SecurityUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * 게시글 템플릿(post template) 관련 API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/posts/templates")
class PostTemplateController(
    private val postTemplateService: PostTemplateService,
) {
    /**
     * 템플릿을 생성합니다.
     * 응답으로 생성된 리소스의 Location 헤더를 반환합니다.
     */
    @PostMapping
    fun createPostTemplate(
        @Valid @RequestBody request: PostTemplateInfoDto,
        @AuthenticationPrincipal user: SecurityUser,
    ): ResponseEntity<Void> {
        val templateId = postTemplateService.createPostTemplate(user.id, request)
        return ResponseEntity.created(URI.create("/api/posts/templates/$templateId")).build()
    }

    /**
     * 특정 템플릿을 단건 조회합니다.
     */
    @GetMapping("/{id}")
    fun getPostTemplate(
        @PathVariable("id") id: Long,
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<Response<PostTemplateInfoDto>> {
        val response = postTemplateService.getTemplate(securityUser.id, id)
        return ResponseEntity.ok(CommonResponse.success(response, "post template retrieved"))
    }

    /**
     * 현재 사용자 기준 템플릿 목록을 조회합니다.
     */
    @GetMapping
    fun getPostTemplates(
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<Response<List<PostTemplateSummaryRes>>> {
        val response = postTemplateService.getTemplateListByMember(securityUser.id)
        return ResponseEntity.ok(CommonResponse.success(response, "post template list retrieved"))
    }

    /**
     * 템플릿을 수정합니다.
     */
    @PutMapping("/{id}")
    fun updatePostTemplate(
        @PathVariable("id") id: Long,
        @AuthenticationPrincipal securityUser: SecurityUser,
        @Valid @RequestBody request: PostTemplateUpdateReq,
    ): ResponseEntity<Void> {
        postTemplateService.updatePostTemplate(securityUser.id, id, request)
        return ResponseEntity.noContent().build()
    }

    /**
     * 템플릿을 삭제합니다.
     */
    @DeleteMapping("/{id}")
    fun deletePostTemplate(
        @PathVariable("id") id: Long,
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<Void> {
        postTemplateService.deleteTemplate(securityUser.id, id)
        return ResponseEntity.noContent().build()
    }
}

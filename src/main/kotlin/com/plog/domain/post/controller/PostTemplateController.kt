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
import org.springframework.web.bind.annotation.*
import java.net.URI

/**
 * postTemplate 관련한 API 엔드포인트입니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
@RestController
@RequestMapping("/api/posts/templates")
class PostTemplateController(private val postTemplateService: PostTemplateService) {

    @PostMapping
    fun createPostTemplate(
        @Valid @RequestBody request: PostTemplateInfoDto,
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<Void> {
        val templateId = postTemplateService.createPostTemplate(user.id, request)
        return ResponseEntity.created(URI.create("/api/posts/templates/$templateId")).build()
    }

    @GetMapping("/{id}")
    fun getPostTemplate(
        @PathVariable(name = "id") id: Long,
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<Response<PostTemplateInfoDto>> {
        val response = postTemplateService.getTemplate(securityUser.id, id)
        return ResponseEntity.ok(CommonResponse.success(response, "post template 조회"))
    }

    @GetMapping
    fun getPostTemplates(
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<Response<List<PostTemplateSummaryRes>>> {
        val response = postTemplateService.getTemplateListByMember(securityUser.id)
        return ResponseEntity.ok(CommonResponse.success(response, "post template 리스트 조회"))
    }

    @PutMapping("/{id}")
    fun updatePostTemplate(
        @PathVariable(name = "id") id: Long,
        @AuthenticationPrincipal securityUser: SecurityUser,
        @Valid @RequestBody request: PostTemplateUpdateReq
    ): ResponseEntity<Response<Void>> {
        postTemplateService.updatePostTemplate(securityUser.id, id, request)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun deletePostTemplate(
        @PathVariable(name = "id") id: Long,
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<Void> {
        postTemplateService.deleteTemplate(securityUser.id, id)
        return ResponseEntity.noContent().build()
    }
}

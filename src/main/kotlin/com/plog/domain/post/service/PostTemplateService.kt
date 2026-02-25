package com.plog.domain.post.service

import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSummaryRes
import com.plog.domain.post.dto.PostTemplateUpdateReq

/**
 * 게시글 템플릿 도메인의 비즈니스 기능 계약입니다.
 */
interface PostTemplateService {
    /** 템플릿을 생성합니다. */
    fun createPostTemplate(memberId: Long, dto: PostTemplateInfoDto): Long

    /** 특정 사용자의 템플릿 목록을 조회합니다. */
    fun getTemplateListByMember(memberId: Long): List<PostTemplateSummaryRes>

    /** 템플릿을 수정합니다. */
    fun updatePostTemplate(memberId: Long, templateId: Long, dto: PostTemplateUpdateReq)

    /** 템플릿 단건을 조회합니다. */
    fun getTemplate(memberId: Long, templateId: Long): PostTemplateInfoDto

    /** 템플릿을 삭제합니다. */
    fun deleteTemplate(memberId: Long, templateId: Long)

    /** 회원 가입 시 기본 템플릿 시드를 초기화합니다. */
    fun initTemplateSeedOfUser(memberId: Long)
}

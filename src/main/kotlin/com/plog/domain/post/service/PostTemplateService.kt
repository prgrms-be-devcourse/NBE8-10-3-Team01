package com.plog.domain.post.service

import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSummaryRes
import com.plog.domain.post.dto.PostTemplateUpdateReq

/**
 * 게시글 템플릿(Post Template)에 대한 비즈니스 기능을 정의하는 서비스 인터페이스입니다.
 *
 * <p>
 * 게시글 템플릿은 사용자가 자주 사용하는 게시글 구조를 저장해두고,
 * 이후 게시글 작성 시 재사용하기 위한 목적의 도메인입니다.
 * 본 인터페이스는 템플릿의 생성, 조회, 수정, 삭제에 대한 기능 계약을 정의합니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
interface PostTemplateService {

    /**
     * 게시글 템플릿을 생성합니다.
     *
     * @param memberId 회원 식별자
     * @param dto 생성할 템플릿 정보
     * @return 생성된 템플릿의 식별자
     */
    fun createPostTemplate(memberId: Long, dto: PostTemplateInfoDto): Long

    fun getTemplateListByMember(memberId: Long): List<PostTemplateSummaryRes>

    fun updatePostTemplate(memberId: Long, templateId: Long, dto: PostTemplateUpdateReq)

    fun getTemplate(memberId: Long, templateId: Long): PostTemplateInfoDto

    fun deleteTemplate(memberId: Long, templateId: Long)

    fun initTemplateSeedOfUser(memberId: Long)
}

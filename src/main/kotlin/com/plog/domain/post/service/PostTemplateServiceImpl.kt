package com.plog.domain.post.service

import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSeed
import com.plog.domain.post.dto.PostTemplateSummaryRes
import com.plog.domain.post.dto.PostTemplateUpdateReq
import com.plog.domain.post.entity.PostTemplate
import com.plog.domain.post.repository.PostTemplateRepository
import com.plog.global.exception.errorCode.PostErrorCode
import com.plog.global.exception.exceptions.PostException
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * 게시글 템플릿(PostTemplate)에 대한 비즈니스 로직을 처리하는 서비스 구현체입니다.
 *
 * <p>
 * 사용자가 저장한 게시글 템플릿을 생성, 조회, 수정, 삭제하는 기능을 제공하며,
 * 모든 변경·조회 작업에 대해 템플릿 소유자 검증을 수행합니다.
 *
 * <p><b>상속 정보:</b><br>
 * [PostTemplateService] 인터페이스를 구현합니다.
 *
 * <p><b>빈 관리:</b><br>
 * `@Service`로 등록되어 Spring 컨테이너에서 관리되며,
 * 트랜잭션 경계는 메서드 단위로 설정됩니다.
 *
 * @author jack8
 * @since 2026-01-26
 */
@Service
class PostTemplateServiceImpl(
    private val postTemplateRepository: PostTemplateRepository,
    private val memberRepository: MemberRepository
) : PostTemplateService {

    var seeds: List<PostTemplateSeed>? = null
        internal set

    @PostConstruct
    fun init() {
        try {
            val resolver = PathMatchingResourcePatternResolver()
            val resources = resolver.getResources("classpath:postTemplate/*.md")
            val loads = mutableListOf<PostTemplateSeed>()

            for (resource in resources) {
                val filename = resource.filename ?: continue

                val title = filename.replace(".md", "").replace("_", " ")
                val context = String(
                    resource.inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
                )

                loads.add(
                    PostTemplateSeed.builder()
                        .title(title)
                        .name(title)
                        .content(context)
                        .build()
                )
            }
            this.seeds = loads.toList()
        } catch (e: IOException) {
            throw IllegalStateException(
                "[PostTemplateServiceImpl#init] failed to load postTemplate/*.md", e
            )
        }
    }

    @Transactional
    override fun createPostTemplate(memberId: Long?, dto: PostTemplateInfoDto?): Long? {
        val author = memberRepository.getReferenceById(memberId!!)

        val postTemplate = PostTemplate.builder()
            .name(dto!!.name)
            .title(dto.title)
            .content(dto.content)
            .member(author)
            .build()

        val saved = postTemplateRepository.save(postTemplate)
        return saved.id
    }

    @Transactional(readOnly = true)
    override fun getTemplateListByMember(memberId: Long?): List<PostTemplateSummaryRes>? {
        val getList = postTemplateRepository.findAllByMember_Id(memberId!!)

        return getList.map { PostTemplateSummaryRes.to(it) }
    }

    @Transactional
    override fun updatePostTemplate(memberId: Long?, templateId: Long?, dto: PostTemplateUpdateReq?) {
        val postTemplate = findByTemplateId(templateId!!)

        validateOwner(memberId!!, postTemplate)

        postTemplate.update(dto!!.name, dto.title, dto.content)

        postTemplateRepository.save(postTemplate)
    }

    @Transactional(readOnly = true)
    override fun getTemplate(memberId: Long?, templateId: Long?): PostTemplateInfoDto? {
        val postTemplate = findByTemplateId(templateId!!)

        validateOwner(memberId!!, postTemplate)

        return PostTemplateInfoDto.to(postTemplate)
    }

    @Transactional
    override fun deleteTemplate(memberId: Long?, templateId: Long?) {
        val postTemplate = findByTemplateId(templateId!!)

        validateOwner(memberId!!, postTemplate)

        postTemplateRepository.delete(postTemplate)
    }

    @Transactional
    override fun initTemplateSeedOfUser(memberId: Long?) {
        val member = memberRepository.getReferenceById(memberId!!)
        val templates = mutableListOf<PostTemplate>()
        seeds?.forEach { seed ->
            templates.add(
                PostTemplate.builder()
                    .member(member)
                    .name(seed.name)
                    .title(seed.title)
                    .content(seed.content)
                    .build()
            )
        }

        postTemplateRepository.saveAll(templates)
    }

    private fun findByTemplateId(id: Long): PostTemplate {
        return postTemplateRepository.findById(id)
            .orElseThrow {
                PostException(
                    PostErrorCode.POST_TEMPLATE_NOT_FOUND,
                    "[PostTemplateServiceImpl#findByTemplateId] unknown template id = $id"
                )
            }
    }

    private fun validateOwner(memberId: Long, template: PostTemplate) {
        if (template.member.id != memberId) {
            throw PostException(
                PostErrorCode.POST_TEMPLATE_AUTH_FAIL,
                "[PostTemplateServiceImpl#validateOwner] request user is $memberId , but actual owner is ${template.member.id}"
            )
        }
    }
}

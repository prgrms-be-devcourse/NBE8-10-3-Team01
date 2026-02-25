package com.plog.domain.post.service

import com.plog.domain.member.entity.Member
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
 * 게시글 템플릿 서비스 구현체입니다.
 *
 * 템플릿 생성/조회/수정/삭제와 사용자 기본 템플릿 초기화 로직을 담당합니다.
 */
@Service
class PostTemplateServiceImpl(
    private val postTemplateRepository: PostTemplateRepository,
    private val memberRepository: MemberRepository,
) : PostTemplateService {

    private var seeds: List<PostTemplateSeed> = emptyList()

    /**
     * 테스트 또는 디버깅 용도로 로드된 시드를 조회합니다.
     */
    fun getSeeds(): List<PostTemplateSeed> = seeds

    /**
     * 클래스패스 postTemplate 폴더의 markdown 템플릿 파일을 읽어 초기 시드로 적재합니다.
     */
    @PostConstruct
    fun init() {
        try {
            val resolver = PathMatchingResourcePatternResolver()
            val resources = resolver.getResources("classpath:postTemplate/*.md")

            seeds = resources.mapNotNull { resource ->
                val filename = resource.filename ?: return@mapNotNull null
                val title = filename.removeSuffix(".md").replace("_", " ")
                val content = resource.inputStream.use {
                    String(it.readAllBytes(), StandardCharsets.UTF_8)
                }

                PostTemplateSeed(
                    name = title,
                    title = title,
                    content = content,
                )
            }
        } catch (e: IOException) {
            throw IllegalStateException(
                "[PostTemplateServiceImpl#init] failed to load postTemplate/*.md",
                e,
            )
        }
    }

    /**
     * 템플릿을 생성하고 생성된 템플릿 ID를 반환합니다.
     */
    @Transactional
    override fun createPostTemplate(memberId: Long, dto: PostTemplateInfoDto): Long {
        val author: Member = memberRepository.getReferenceById(memberId)

        val postTemplate = PostTemplate.builder()
            .name(dto.name)
            .title(dto.title)
            .content(dto.content)
            .member(author)
            .build()

        val saved = postTemplateRepository.save(postTemplate)
        return requireNotNull(saved.id) {
            "[PostTemplateServiceImpl#createPostTemplate] template id should not be null after save"
        }
    }

    /**
     * 사용자 ID 기준 템플릿 목록을 요약 형태로 반환합니다.
     */
    @Transactional(readOnly = true)
    override fun getTemplateListByMember(memberId: Long): List<PostTemplateSummaryRes> {
        return postTemplateRepository.findAllByMember_Id(memberId).map(PostTemplateSummaryRes::to)
    }

    /**
     * 소유권 검증 후 템플릿을 수정합니다.
     */
    @Transactional
    override fun updatePostTemplate(memberId: Long, templateId: Long, dto: PostTemplateUpdateReq) {
        val postTemplate = findByTemplateId(templateId)
        validateOwner(memberId, postTemplate)

        postTemplate.update(dto.name, dto.title, dto.content)
        postTemplateRepository.save(postTemplate)
    }

    /**
     * 소유권 검증 후 템플릿 단건을 반환합니다.
     */
    @Transactional(readOnly = true)
    override fun getTemplate(memberId: Long, templateId: Long): PostTemplateInfoDto {
        val postTemplate = findByTemplateId(templateId)
        validateOwner(memberId, postTemplate)
        return PostTemplateInfoDto.to(postTemplate)
    }

    /**
     * 소유권 검증 후 템플릿을 삭제합니다.
     */
    @Transactional
    override fun deleteTemplate(memberId: Long, templateId: Long) {
        val postTemplate = findByTemplateId(templateId)
        validateOwner(memberId, postTemplate)
        postTemplateRepository.delete(postTemplate)
    }

    /**
     * 회원 가입 직후 기본 템플릿 시드를 사용자 소유 템플릿으로 저장합니다.
     */
    @Transactional
    override fun initTemplateSeedOfUser(memberId: Long) {
        val member = memberRepository.getReferenceById(memberId)
        val templates = seeds.map { seed ->
            PostTemplate.builder()
                .member(member)
                .name(seed.name)
                .title(seed.title)
                .content(seed.content)
                .build()
        }

        postTemplateRepository.saveAll(templates)
    }

    /**
     * 템플릿 ID로 엔티티를 조회하고 없으면 예외를 던집니다.
     */
    private fun findByTemplateId(id: Long): PostTemplate {
        return postTemplateRepository.findById(id)
            .orElseThrow {
                PostException(
                    PostErrorCode.POST_TEMPLATE_NOT_FOUND,
                    "[PostTemplateServiceImpl#findByTemplateId] unknown template id = $id",
                )
            }
    }

    /**
     * 요청 사용자가 템플릿 소유자인지 검증합니다.
     */
    private fun validateOwner(memberId: Long, template: PostTemplate) {
        val ownerId = template.member.id
        if (ownerId != memberId) {
            throw PostException(
                PostErrorCode.POST_TEMPLATE_AUTH_FAIL,
                "[PostTemplateServiceImpl#validateOwner] request user is $memberId , but actual owner is $ownerId",
            )
        }
    }
}

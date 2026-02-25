package com.plog.domain.post.service

import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSeed
import com.plog.domain.post.dto.PostTemplateUpdateReq
import com.plog.domain.post.entity.PostTemplate
import com.plog.domain.post.repository.PostTemplateRepository
import com.plog.global.exception.errorCode.PostErrorCode
import com.plog.global.exception.exceptions.PostException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.any
import org.mockito.Mockito.never

@ExtendWith(MockitoExtension::class)
class PostTemplateServiceTest {

    @Mock
    lateinit var postTemplateRepository: PostTemplateRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    private lateinit var service: PostTemplateServiceImpl

    private val memberId = 1L
    private val otherMemberId = 2L
    private val templateId = 10L

    @BeforeEach
    fun setUp() {
        service = PostTemplateServiceImpl(postTemplateRepository, memberRepository)

        val seeds = listOf(
            PostTemplateSeed.builder()
                .name("seed")
                .title("Seed")
                .content("content")
                .build(),
        )
        ReflectionTestUtils.setField(service, "seeds", seeds)
    }

    private fun memberEntity(id: Long): Member {
        val member = Member.builder()
            .email("member$id@plog.com")
            .password("password")
            .nickname("member$id")
            .build()
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }

    private fun templateEntity(id: Long, ownerId: Long): PostTemplate {
        val template = PostTemplate.builder()
            .name("template-name")
            .title("template-title")
            .content("template-content")
            .member(memberEntity(ownerId))
            .build()

        ReflectionTestUtils.setField(template, "id", id)
        return template
    }

    @Test
    @DisplayName("템플릿 생성 성공")
    fun createPostTemplateSuccess() {
        val authorRef = memberEntity(memberId)
        given(memberRepository.getReferenceById(memberId)).willReturn(authorRef)

        val dto = PostTemplateInfoDto(
            id = 1L,
            name = "name",
            title = "title",
            content = "content",
        )

        val saved = templateEntity(100L, memberId)
        given(postTemplateRepository.save(any(PostTemplate::class.java))).willReturn(saved)

        val result = service.createPostTemplate(memberId, dto)

        assertThat(result).isEqualTo(100L)
        then(memberRepository).should().getReferenceById(memberId)
        then(postTemplateRepository).should().save(any(PostTemplate::class.java))
    }

    @Test
    @DisplayName("템플릿 리스트 조회 성공")
    fun getTemplateListSuccess() {
        val t1 = templateEntity(1L, memberId)
        val t2 = templateEntity(2L, memberId)
        given(postTemplateRepository.findAllByMember_Id(memberId)).willReturn(listOf(t1, t2))

        val result = service.getTemplateListByMember(memberId)

        assertThat(result).hasSize(2)
        then(postTemplateRepository).should().findAllByMember_Id(memberId)
    }

    @Test
    @DisplayName("템플릿 단건 조회 성공 - 소유자 일치")
    fun getTemplateSuccess() {
        val template = templateEntity(templateId, memberId)
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(template))

        val result = service.getTemplate(memberId, templateId)

        assertThat(result).isNotNull
        then(postTemplateRepository).should().findById(templateId)
    }

    @Test
    @DisplayName("템플릿 단건 조회 실패 - 존재하지 않음")
    fun getTemplateNotFound() {
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.empty())

        assertThatThrownBy { service.getTemplate(memberId, templateId) }
            .isInstanceOf(PostException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_TEMPLATE_NOT_FOUND)

        then(postTemplateRepository).should().findById(templateId)
    }

    @Test
    @DisplayName("템플릿 단건 조회 실패 - 소유자 불일치")
    fun getTemplateFailNotOwner() {
        val template = templateEntity(templateId, otherMemberId)
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(template))

        assertThatThrownBy { service.getTemplate(memberId, templateId) }
            .isInstanceOf(PostException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_TEMPLATE_AUTH_FAIL)

        then(postTemplateRepository).should().findById(templateId)
    }

    @Test
    @DisplayName("템플릿 수정 성공")
    fun updateTemplateSuccess() {
        val template = templateEntity(templateId, memberId)
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(template))
        val req = PostTemplateUpdateReq("n", "t", "c")

        service.updatePostTemplate(memberId, templateId, req)

        then(postTemplateRepository).should().save(template)
        assertThat(template.name).isEqualTo("n")
        assertThat(template.title).isEqualTo("t")
        assertThat(template.content).isEqualTo("c")
    }

    @Test
    @DisplayName("템플릿 수정 실패 - 소유자 불일치")
    fun updateTemplateFailNotOwner() {
        val template = templateEntity(templateId, otherMemberId)
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(template))
        val req = PostTemplateUpdateReq("n", "t", "c")

        assertThatThrownBy { service.updatePostTemplate(memberId, templateId, req) }
            .isInstanceOf(PostException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_TEMPLATE_AUTH_FAIL)

        then(postTemplateRepository).should(never()).save(any())
    }

    @Test
    @DisplayName("템플릿 삭제 성공")
    fun deleteTemplateSuccess() {
        val template = templateEntity(templateId, memberId)
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(template))

        service.deleteTemplate(memberId, templateId)

        then(postTemplateRepository).should().delete(template)
    }

    @Test
    @DisplayName("템플릿 삭제 실패 - 소유자 불일치")
    fun deleteTemplateFailNotOwner() {
        val template = templateEntity(templateId, otherMemberId)
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(template))

        assertThatThrownBy { service.deleteTemplate(memberId, templateId) }
            .isInstanceOf(PostException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_TEMPLATE_AUTH_FAIL)

        then(postTemplateRepository).should(never()).delete(any())
    }
}

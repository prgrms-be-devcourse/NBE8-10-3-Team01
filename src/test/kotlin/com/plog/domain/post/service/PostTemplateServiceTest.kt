package com.plog.domain.post.service

import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.dto.PostTemplateInfoDto
import com.plog.domain.post.dto.PostTemplateSummaryRes
import com.plog.domain.post.dto.PostTemplateUpdateReq
import com.plog.domain.post.entity.PostTemplate
import com.plog.domain.post.repository.PostTemplateRepository
import com.plog.global.exception.exceptions.PostException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
class PostTemplateServiceTest {

    @InjectMocks
    lateinit var postTemplateService: PostTemplateServiceImpl

    @Mock
    lateinit var postTemplateRepository: PostTemplateRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    @Test
    @DisplayName("게시글 템플릿 생성 성공")
    fun createPostTemplateSuccess() {
        // [Given]
        val memberId = 1L
        val dto = PostTemplateInfoDto(null, "이름", "제목", "내용")
        val member = Member()
        ReflectionTestUtils.setField(member, "id", memberId)

        whenever(memberRepository.getReferenceById(memberId)).thenReturn(member)
        whenever(postTemplateRepository.save(any<PostTemplate>())).thenAnswer { invocation ->
            val pt = invocation.getArgument<PostTemplate>(0)
            ReflectionTestUtils.setField(pt, "id", 100L)
            pt
        }

        // [When]
        val resultId = postTemplateService.createPostTemplate(memberId, dto)

        // [Then]
        assertThat(resultId).isEqualTo(100L)
        verify(postTemplateRepository).save(any<PostTemplate>())
    }

    @Test
    @DisplayName("회원별 템플릿 목록 조회 성공")
    fun getTemplateListByMemberSuccess() {
        // [Given]
        val memberId = 1L
        val member = Member()
        ReflectionTestUtils.setField(member, "id", memberId)

        val pt = PostTemplate("이름", "제목", "내용", member)
        ReflectionTestUtils.setField(pt, "id", 100L)

        whenever(postTemplateRepository.findAllByMember_Id(memberId)).thenReturn(listOf(pt))

        // [When]
        val results = postTemplateService.getTemplateListByMember(memberId)

        // [Then]
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("이름")
        assertThat(results[0].id).isEqualTo(100L)
    }

    @Test
    @DisplayName("템플릿 상세 조회 성공")
    fun getTemplateSuccess() {
        // [Given]
        val memberId = 1L
        val templateId = 100L
        val member = Member()
        ReflectionTestUtils.setField(member, "id", memberId)

        val pt = PostTemplate("이름", "제목", "내용", member)
        ReflectionTestUtils.setField(pt, "id", templateId)

        whenever(postTemplateRepository.findById(templateId)).thenReturn(Optional.of(pt))

        // [When]
        val result = postTemplateService.getTemplate(memberId, templateId)

        // [Then]
        assertThat(result.name).isEqualTo("이름")
        assertThat(result.title).isEqualTo("제목")
        assertThat(result.content).isEqualTo("내용")
    }

    @Test
    @DisplayName("자신의 템플릿이 아닌 경우 접근 시 PostException 발생")
    fun getTemplateFailNotOwner() {
        // [Given]
        val ownerId = 1L
        val otherMemberId = 2L
        val templateId = 100L
        val owner = Member()
        ReflectionTestUtils.setField(owner, "id", ownerId)

        val pt = PostTemplate("이름", "제목", "내용", owner)

        whenever(postTemplateRepository.findById(templateId)).thenReturn(Optional.of(pt))

        // [When & Then]
        assertThatThrownBy { postTemplateService.getTemplate(otherMemberId, templateId) }
            .isInstanceOf(PostException::class.java)
    }

    @Test
    @DisplayName("템플릿 수정 성공")
    fun updatePostTemplateSuccess() {
        // [Given]
        val memberId = 1L
        val templateId = 100L
        val member = Member()
        ReflectionTestUtils.setField(member, "id", memberId)

        val pt = PostTemplate("기존이름", "기존제목", "기존내용", member)
        whenever(postTemplateRepository.findById(templateId)).thenReturn(Optional.of(pt))

        val updateReq = PostTemplateUpdateReq("새이름", "새제목", "새내용")

        // [When]
        postTemplateService.updatePostTemplate(memberId, templateId, updateReq)

        // [Then]
        assertThat(pt.name).isEqualTo("새이름")
        assertThat(pt.title).isEqualTo("새제목")
        assertThat(pt.content).isEqualTo("새내용")
    }

    @Test
    @DisplayName("템플릿 삭제 성공")
    fun deleteTemplateSuccess() {
        // [Given]
        val memberId = 1L
        val templateId = 100L
        val member = Member()
        ReflectionTestUtils.setField(member, "id", memberId)

        val pt = PostTemplate("이름", "제목", "내용", member)
        whenever(postTemplateRepository.findById(templateId)).thenReturn(Optional.of(pt))

        // [When]
        postTemplateService.deleteTemplate(memberId, templateId)

        // [Then]
        verify(postTemplateRepository).delete(pt)
    }
}

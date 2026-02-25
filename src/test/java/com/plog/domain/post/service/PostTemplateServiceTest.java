package com.plog.domain.post.service;

import com.plog.domain.member.entity.Member;
import com.plog.domain.member.repository.MemberRepository;
import com.plog.domain.post.dto.PostTemplateInfoDto;
import com.plog.domain.post.dto.PostTemplateSummaryRes;
import com.plog.domain.post.dto.PostTemplateUpdateReq;
import com.plog.domain.post.entity.PostTemplate;
import com.plog.domain.post.repository.PostTemplateRepository;
import com.plog.global.exception.exceptions.PostException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class PostTemplateServiceTest {

    @InjectMocks
    private PostTemplateServiceImpl postTemplateService;

    @Mock
    private PostTemplateRepository postTemplateRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("게시글 템플릿 생성 성공")
    void createPostTemplateSuccess() {
        // [Given]
        Long memberId = 1L;
        PostTemplateInfoDto dto = new PostTemplateInfoDto(null, "이름", "제목", "내용");
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        given(memberRepository.getReferenceById(memberId)).willReturn(member);
        given(postTemplateRepository.save(any(PostTemplate.class))).willAnswer(invocation -> {
            PostTemplate pt = invocation.getArgument(0);
            ReflectionTestUtils.setField(pt, "id", 100L);
            return pt;
        });

        // [When]
        Long resultId = postTemplateService.createPostTemplate(memberId, dto);

        // [Then]
        assertThat(resultId).isEqualTo(100L);
        verify(postTemplateRepository).save(any(PostTemplate.class));
    }

    @Test
    @DisplayName("회원별 템플릿 목록 조회 성공")
    void getTemplateListByMemberSuccess() {
        // [Given]
        Long memberId = 1L;
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        PostTemplate pt = new PostTemplate("이름", "제목", "내용", member);
        ReflectionTestUtils.setField(pt, "id", 100L);

        given(postTemplateRepository.findAllByMember_Id(memberId)).willReturn(List.of(pt));

        // [When]
        List<PostTemplateSummaryRes> results = postTemplateService.getTemplateListByMember(memberId);

        // [Then]
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("이름");
        assertThat(results.get(0).id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("템플릿 상세 조회 성공")
    void getTemplateSuccess() {
        // [Given]
        Long memberId = 1L;
        Long templateId = 100L;
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        PostTemplate pt = new PostTemplate("이름", "제목", "내용", member);
        ReflectionTestUtils.setField(pt, "id", templateId);

        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(pt));

        // [When]
        PostTemplateInfoDto result = postTemplateService.getTemplate(memberId, templateId);

        // [Then]
        assertThat(result.name()).isEqualTo("이름");
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.content()).isEqualTo("내용");
    }

    @Test
    @DisplayName("자신의 템플릿이 아닌 경우 접근 시 PostException 발생")
    void getTemplateFailNotOwner() {
        // [Given]
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long templateId = 100L;
        Member owner = Member.builder().build();
        ReflectionTestUtils.setField(owner, "id", ownerId);

        PostTemplate pt = new PostTemplate("이름", "제목", "내용", owner);

        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(pt));

        // [When & Then]
        assertThatThrownBy(() -> postTemplateService.getTemplate(otherMemberId, templateId))
                .isInstanceOf(PostException.class);
    }

    @Test
    @DisplayName("템플릿 수정 성공")
    void updatePostTemplateSuccess() {
        // [Given]
        Long memberId = 1L;
        Long templateId = 100L;
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        PostTemplate pt = new PostTemplate("기존이름", "기존제목", "기존내용", member);
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(pt));

        PostTemplateUpdateReq updateReq = new PostTemplateUpdateReq("새이름", "새제목", "새내용");

        // [When]
        postTemplateService.updatePostTemplate(memberId, templateId, updateReq);

        // [Then]
        assertThat(pt.getName()).isEqualTo("새이름");
        assertThat(pt.getTitle()).isEqualTo("새제목");
        assertThat(pt.getContent()).isEqualTo("새내용");
    }

    @Test
    @DisplayName("템플릿 삭제 성공")
    void deleteTemplateSuccess() {
        // [Given]
        Long memberId = 1L;
        Long templateId = 100L;
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        PostTemplate pt = new PostTemplate("이름", "제목", "내용", member);
        given(postTemplateRepository.findById(templateId)).willReturn(Optional.of(pt));

        // [When]
        postTemplateService.deleteTemplate(memberId, templateId);

        // [Then]
        verify(postTemplateRepository).delete(pt);
    }
}

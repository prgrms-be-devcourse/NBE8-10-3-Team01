package com.plog.domain.post.service;

import com.plog.domain.member.entity.Member;
import com.plog.domain.post.dto.PostInfoRes;
import com.plog.domain.post.entity.Post;
import com.plog.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class PostServiceTest {
    @InjectMocks
    private PostServiceImpl postService;

    @Mock
    private PostRepository postRepository;

    @Test
    @DisplayName("게시글 저장 시 마크다운이 제거된 요약글이 자동 생성")
    void createPostSuccess() {
        String title = "테스트 제목";
        String content = "# Hello\n**Spring Boot**";

        Post mockPost = Post.builder()
                .title(title)
                .content(content)
                .build();

        given(postRepository.save(ArgumentMatchers.any(Post.class)))
                .willReturn(mockPost);

        postService.createPost(title, content);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());

        Post savedPost = postCaptor.getValue();
        assertThat(savedPost.getSummary()).isEqualTo("Hello\nSpring Boot");
    }

    @Test
    @DisplayName("본문이 150자를 초과하면 요약글은 150자까지만 저장되고 말줄임표가 붙는다")
    void createPostSuccessSummaryTruncation() {
        String title = "제목";
        String longContent = "가".repeat(200);

        Post mockPost = Post.builder()
                .title(title)
                .content(longContent)
                .build();

        given(postRepository.save(ArgumentMatchers.any(Post.class)))
                .willReturn(mockPost);

        postService.createPost(title, longContent);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        Post savedPost = postCaptor.getValue();

        assertThat(savedPost.getSummary().length()).isEqualTo(153);
        assertThat(savedPost.getSummary()).endsWith("...");
    }

    @Test
    @DisplayName("회원 ID로 조회 시 엔티티가 PostInfoRes의 모든 필드로 올바르게 변환되어야 한다")
    void getPostsByMemberSuccess() {
        // [Given]
        Long memberId = 1L;
        // 페이징 정보 설정 (0페이지, 10개씩 조회)
        Pageable pageable = PageRequest.of(0, 10);

        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 본문")
                .summary("테스트 요약")
                .viewCount(10)
                .build();

        // SliceImpl을 사용하여 리포지토리 반환값 모킹 (데이터 1개, 다음 페이지 없음)
        Slice<Post> mockSlice = new SliceImpl<>(List.of(post), pageable, false);

        given(postRepository.findAllByMemberId(memberId, pageable))
                .willReturn(mockSlice);

        // [When]
        Slice<PostInfoRes> result = postService.getPostsByMember(memberId, pageable);

        // [Then]
        // 1. Slice 자체에 대한 검증
        assertThat(result.getContent()).hasSize(1); // 실제 데이터 개수 확인
        assertThat(result.hasNext()).isFalse();    // 다음 페이지 여부 확인

        // 2. DTO 필드 매핑 검증 (첫 번째 요소 추출)
        PostInfoRes dto = result.getContent().get(0);
        assertThat(dto.title()).isEqualTo("테스트 제목");
        assertThat(dto.content()).isEqualTo("테스트 본문");
        assertThat(dto.summary()).isEqualTo("테스트 요약");
        assertThat(dto.viewCount()).isEqualTo(10);

        // 3. 리포지토리 호출 확인 (새로운 메서드와 파라미터 기준)
        verify(postRepository).findAllByMemberId(memberId, pageable);
    }
}

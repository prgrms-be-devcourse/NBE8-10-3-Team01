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
        LocalDateTime now = LocalDateTime.now();

        // PostInfoRes의 모든 필드에 대응하는 데이터를 가진 Post 엔티티 생성
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 본문")
                .summary("테스트 요약")
                .viewCount(10)
                .build();

        // BaseEntity 필드(id, createDate, modifyDate)는 Mockito로 시뮬레이션하거나
        // 테스트용 별도 setter/reflection을 사용해야 하지만, 여기서는 필드 매핑 로직 확인에 집중합니다.
        given(postRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId))
                .willReturn(List.of(post));

        // [When]
        List<PostInfoRes> result = postService.getPostsByMember(memberId);

        // [Then]
        assertThat(result).hasSize(1);
        PostInfoRes dto = result.get(0);

        // PostInfoRes 레코드의 필드 접근자(Accessor)를 사용하여 검증
        assertThat(dto.title()).isEqualTo("테스트 제목");
        assertThat(dto.content()).isEqualTo("테스트 본문");
        assertThat(dto.summary()).isEqualTo("테스트 요약");
        assertThat(dto.viewCount()).isEqualTo(10);

        verify(postRepository).findAllByMemberIdOrderByCreatedAtDesc(memberId);
    }
}

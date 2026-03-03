package com.plog.domain.post.service

import com.plog.domain.comment.repository.CommentRepository
import com.plog.domain.hashtag.entity.HashTag
import com.plog.domain.hashtag.entity.PostHashTag
import com.plog.domain.hashtag.repository.HashTagRepository
import com.plog.domain.hashtag.repository.PostHashTagRepository
import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.dto.PostCreateReq
import com.plog.domain.post.dto.PostInfoRes
import com.plog.domain.post.dto.PostListRes
import com.plog.domain.post.dto.PostUpdateReq
import com.plog.domain.post.entity.Post
import com.plog.domain.post.repository.PostRepository
import com.plog.domain.post.repository.ViewCountRedisRepository
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.exception.exceptions.PostException
import com.plog.global.util.HashUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
class PostServiceTest {

    @InjectMocks
    lateinit var postService: PostServiceImpl

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var commentRepository: CommentRepository

    @Mock
    lateinit var postHashTagRepository: PostHashTagRepository

    @Mock
    lateinit var hashTagRepository: HashTagRepository

    @Mock
    lateinit var imageRepository: ImageRepository

    @Mock
    lateinit var viewCountRedisRepository: ViewCountRedisRepository

    @BeforeEach
    fun setUp() {
        lenient().whenever(imageRepository.findAllByAccessUrlIn(any())).thenReturn(emptyList())
    }

    @Test
    @DisplayName("게시글 저장 시 마크다운이 제거된 요약글이 자동 생성")
    fun createPostSuccess() {
        val memberId = 1L
        val requestDto = PostCreateReq("테스트 제목", "# Hello\n**Spring Boot**", emptyList(), "example.com")

        val mockMember = Member.builder().build()
        ReflectionTestUtils.setField(mockMember, "id", memberId)

        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)

        whenever(postRepository.save(any<Post>()))
            .thenAnswer { invocation ->
                val post = invocation.getArgument<Post>(0)
                ReflectionTestUtils.setField(post, "id", 100L)
                post
            }

        postService.createPost(memberId, requestDto)

        val postCaptor = argumentCaptor<Post>()
        verify(postRepository).save(postCaptor.capture())

        val savedPost = postCaptor.firstValue
        assertThat(savedPost.title).isEqualTo("테스트 제목")
        assertThat(savedPost.summary).isEqualTo("Hello\nSpring Boot")
        assertThat(savedPost.member.id).isEqualTo(memberId)
    }

    @Test
    @DisplayName("본문이 150자를 초과하면 요약글은 150자까지만 저장되고 말줄임표가 붙는다")
    fun createPostSuccessSummaryTruncation() {
        val memberId = 1L
        val longContent = "가".repeat(200)
        val requestDto = PostCreateReq("제목", longContent, emptyList(), "example.com")

        val mockMember = Member.builder().build()
        ReflectionTestUtils.setField(mockMember, "id", memberId)

        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)

        whenever(postRepository.save(any<Post>()))
            .thenAnswer { invocation ->
                val post = invocation.getArgument<Post>(0)
                ReflectionTestUtils.setField(post, "id", 100L)
                post
            }

        postService.createPost(memberId, requestDto)

        val postCaptor = argumentCaptor<Post>()
        verify(postRepository).save(postCaptor.capture())

        val savedPost = postCaptor.firstValue
        assertThat(savedPost.summary?.length).isEqualTo(153)
        assertThat(savedPost.summary).endsWith("...")
    }

    @Test
    @DisplayName("전체 게시글 조회 시 리포지토리의 결과를 Slice DTO로 변환하여 반환한다")
    fun getPostsSuccess() {
        // [Given]
        val author = Member("email", "password", "nickname", null)
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        val post = Post.builder()
            .title("테스트 제목")
            .content("테스트 내용")
            .member(author)
            .build()

        val mockPage = PageImpl(listOf(post), pageable, 1)
        whenever(postRepository.findAllWithMember(any())).thenReturn(mockPage)

        // [When]
        val result = postService.getPosts(pageable)

        // [Then]
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].title).isEqualTo("테스트 제목")
        assertThat(result.number).isEqualTo(0)
        assertThat(result.isLast).isTrue()

        verify(postRepository).findAllWithMember(pageable)
    }

    @Test
    @DisplayName("게시글 상세 조회 시 ViewCountRedisRepository의 증가 로직이 호출되어야 한다 (해싱 적용)")
    fun getPostDetailIncrementsViewCount() {
        // [Given]
        val postId = 1L
        val userId = "user1"
        val hashedUserId = HashUtils.sha256(userId)
        val author = Member.builder().build()
        val post = Post.builder()
            .title("제목")
            .content("내용")
            .member(author)
            .viewCount(0)
            .build()

        whenever(postRepository.findByIdWithMember(postId)).thenReturn(Optional.of(post))
        whenever(commentRepository.findCommentsWithMemberAndImageByPostId(eq(postId), any()))
            .thenReturn(SliceImpl(emptyList()))

        whenever(viewCountRedisRepository.setIfAbsentWithTtl(eq(postId), eq(hashedUserId), any<Long>())).thenReturn(true)

        // [When]
        postService.getPostDetail(postId, userId, 0)

        // [Then]
        verify(viewCountRedisRepository).setIfAbsentWithTtl(eq(postId), eq(hashedUserId), any<Long>())
        verify(viewCountRedisRepository).incrementCount(postId)
        verify(viewCountRedisRepository).addToPending(postId)
    }

    @Test
    @DisplayName("게시글 상세 조회 시 존재하지 않는 ID면 PostException 발생")
    fun getPostDetailNotFound() {
        // [Given]
        whenever(postRepository.findByIdWithMember(any())).thenReturn(Optional.empty())

        // [When & Then]
        assertThatThrownBy { postService.getPostDetail(99L, "user1", 0) }
            .isInstanceOf(PostException::class.java)
    }

    @Test
    @DisplayName("해시태그 적용 시 기존에 존재하는 태그는 재사용하고 새로운 태그는 생성한다")
    fun applyTagsLogic() {
        // [Given]
        val memberId = 1L
        val tagNames = listOf("Spring", "Kotlin")
        val requestDto = PostCreateReq("제목", "내용", tagNames, null)

        val mockMember = Member.builder().build()
        ReflectionTestUtils.setField(mockMember, "id", memberId)
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)

        whenever(postRepository.save(any<Post>())).thenAnswer { invocation ->
            val post = invocation.getArgument<Post>(0)
            ReflectionTestUtils.setField(post, "id", 100L)
            post
        }

        // "spring"은 이미 존재한다고 가정
        val existingTag = HashTag("spring")
        ReflectionTestUtils.setField(existingTag, "id", 1L)
        whenever(hashTagRepository.findByName("spring")).thenReturn(existingTag)

        // "kotlin"은 존재하지 않아 새로 생성된다고 가정
        whenever(hashTagRepository.findByName("kotlin")).thenReturn(null)
        whenever(hashTagRepository.save(any<HashTag>())).thenAnswer { invocation ->
            val tag = invocation.getArgument<HashTag>(0)
            ReflectionTestUtils.setField(tag, "id", 2L)
            tag
        }

        // [When]
        postService.createPost(memberId, requestDto)

        // [Then]
        verify(hashTagRepository).save(argThat { name == "kotlin" })
        verify(postHashTagRepository, times(2)).save(any<PostHashTag>())
    }

    @Test
    @DisplayName("게시글 수정 시 본문에 맞춰 요약본이 새롭게 생성되어야 한다")
    fun updatePostSuccess() {
        // [Given]
        val memberId = 1L
        val postId = 1L

        val member = Member.builder().build()
        ReflectionTestUtils.setField(member, "id", memberId)

        val existingPost = Post.builder()
            .title("기존 제목")
            .content("기존 본문")
            .member(member)
            .summary("기존 요약")
            .build()

        whenever(postRepository.findById(postId)).thenReturn(Optional.of(existingPost))

        val newTitle = "수정된 제목"
        val newContent = "수정된 본문 내용입니다. 이 내용은 150자 미만이므로 그대로 요약이 됩니다."

        // [When]
        postService.updatePost(memberId, postId, PostUpdateReq(newTitle, newContent, null, null))

        // [Then]
        assertThat(existingPost.title).isEqualTo(newTitle)
        assertThat(existingPost.content).isEqualTo(newContent)
        assertThat(existingPost.summary).contains("수정된 본문")
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시 PostException이 발생한다")
    fun updatePostFailNotFound() {
        // [Given]
        val memberId = 1L
        whenever(postRepository.findById(any())).thenReturn(Optional.empty())

        // [When & Then]
        assertThatThrownBy { postService.updatePost(memberId, 99L, PostUpdateReq("제목", "내용", null, null)) }
            .isInstanceOf(PostException::class.java)
            .hasMessageContaining("존재하지 않는 게시물입니다.")
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 게시글 수정을 시도하면, AuthException이 발생한다")
    fun updatePostFailForbidden() {
        // [Given]
        val ownerId = 1L
        val otherMemberId = 2L
        val postId = 1L

        val owner = Member.builder().build()
        ReflectionTestUtils.setField(owner, "id", ownerId)

        val post = Post.builder().member(owner).build()
        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))

        // [When & Then]
        assertThatThrownBy { postService.updatePost(otherMemberId, postId, PostUpdateReq("제목", "내용", null, null)) }
            .isInstanceOf(AuthException::class.java)
            .hasMessageContaining("수정할 권한이 없습니다.")
    }

    @Test
    @DisplayName("게시글 삭제 시 해당 ID의 게시글이 존재하면 삭제를 수행한다")
    fun deletePostSuccess() {
        // [Given]
        val memberId = 1L
        val postId = 1L

        val member = Member.builder().build()
        ReflectionTestUtils.setField(member, "id", memberId)

        val post = Post.builder()
            .title("삭제될 제목")
            .content("삭제될 본문")
            .member(member)
            .build()

        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))

        // [When]
        postService.deletePost(memberId, postId)

        // [Then]
        verify(postRepository).findById(postId)
        verify(postRepository).delete(post)
        verify(commentRepository).deleteParentsByPostId(postId)
        verify(commentRepository).deleteRepliesByPostId(postId)
    }

    @Test
    @DisplayName("존재하지 않는 ID로 삭제 요청 시 PostException이 발생한다")
    fun deletePostFailNotFound() {
        // [Given]
        val memberId = 1L
        val postId = 99L

        whenever(postRepository.findById(any())).thenReturn(Optional.empty())

        // [When & Then]
        assertThatThrownBy { postService.deletePost(memberId, postId) }
            .isInstanceOf(PostException::class.java)
            .hasMessageContaining("존재하지 않는 게시물입니다.")

        verify(postRepository, never()).delete(any<Post>())
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 게시글 삭제를 시도하면 AuthException이 발생한다")
    fun deletePostFailForbidden() {
        // [Given]
        val ownerId = 1L
        val otherMemberId = 2L
        val postId = 1L

        val owner = Member.builder().build()
        ReflectionTestUtils.setField(owner, "id", ownerId)

        val post = Post.builder().member(owner).build()
        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))

        // [When & Then]
        assertThatThrownBy { postService.deletePost(otherMemberId, postId) }
            .isInstanceOf(AuthException::class.java)
            .hasMessageContaining("삭제할 권한이 없습니다.")

        verify(postRepository, never()).delete(any<Post>())
    }

    @Test
    @DisplayName("회원 ID로 조회 시 엔티티가 PostInfoRes의 모든 필드로 올바르게 변환되어야 한다")
    fun getPostsByMemberSuccess() {
        // [Given]
        val memberId = 1L
        val author = Member("email", "password", "nickname", null)
        val pageable = PageRequest.of(0, 10)

        val post = Post.builder()
            .title("테스트 제목")
            .content("테스트 본문")
            .summary("테스트 요약")
            .member(author)
            .viewCount(10)
            .build()

        val mockSlice = SliceImpl(listOf(post), pageable, false)
        whenever(postRepository.findAllByMemberId(memberId, pageable)).thenReturn(mockSlice)

        // [When]
        val result = postService.getPostsByMember(memberId, pageable)

        // [Then]
        assertThat(result.content).hasSize(1)
        assertThat(result.hasNext()).isFalse()

        val dto = result.content[0]
        assertThat(dto.title).isEqualTo("테스트 제목")
        assertThat(dto.content).isEqualTo("테스트 본문")
        assertThat(dto.viewCount).isEqualTo(10)

        verify(postRepository).findAllByMemberId(memberId, pageable)
    }

    @Test
    @DisplayName("content에 이미지 URL이 있으면 해당 이미지를 USED로 마킹한다")
    fun createPost_withImageInContent_marksImageAsUsed() {
        // given
        val memberId = 1L
        val content = "<img src=\"http://minio/bucket/test.jpg\" />"
        val req = PostCreateReq("제목", content, emptyList(), null)

        val mockMember = Member.builder().build()
        ReflectionTestUtils.setField(mockMember, "id", memberId)
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)
        whenever(postRepository.save(any<Post>())).thenAnswer { invocation ->
            val post = invocation.getArgument<Post>(0)
            ReflectionTestUtils.setField(post, "id", 100L)
            post
        }

        val mockImage = mock<Image>()
        whenever(imageRepository.findAllByAccessUrlIn(any())).thenReturn(listOf(mockImage))

        // when
        postService.createPost(memberId, req)

        // then
        verify(imageRepository).findAllByAccessUrlIn(any())
        verify(mockImage).status = Image.ImageStatus.USED
        verify(mockImage).domain = Image.ImageDomain.POST
        verify(mockImage).domainId = 100L
    }

    @Test
    @DisplayName("thumbnail URL이 있으면 해당 이미지도 USED로 마킹한다")
    fun createPost_withThumbnail_marksThumbnailAsUsed() {
        // given
        val memberId = 1L
        val thumbnail = "http://minio/bucket/thumb.jpg"
        val req = PostCreateReq("제목", "본문 내용", emptyList(), thumbnail)

        val mockMember = Member.builder().build()
        ReflectionTestUtils.setField(mockMember, "id", memberId)
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)
        whenever(postRepository.save(any<Post>())).thenAnswer { invocation ->
            val post = invocation.getArgument<Post>(0)
            ReflectionTestUtils.setField(post, "id", 100L)
            post
        }

        val mockImage = mock<Image>()
        whenever(imageRepository.findAllByAccessUrlIn(any())).thenReturn(listOf(mockImage))

        // when
        postService.createPost(memberId, req)

        // then
        verify(imageRepository).findAllByAccessUrlIn(argThat { contains(thumbnail) })
        verify(mockImage).status = Image.ImageStatus.USED
    }

    @Test
    @DisplayName("content와 thumbnail 모두 이미지 없으면 imageRepository를 호출하지 않는다")
    fun createPost_noImages_doesNotCallImageRepository() {
        // given
        val memberId = 1L
        val req = PostCreateReq("제목", "이미지 없는 순수 텍스트", emptyList(), null)

        val mockMember = Member.builder().build()
        ReflectionTestUtils.setField(mockMember, "id", memberId)
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)
        whenever(postRepository.save(any<Post>())).thenAnswer { invocation ->
            val post = invocation.getArgument<Post>(0)
            ReflectionTestUtils.setField(post, "id", 100L)
            post
        }

        // when
        postService.createPost(memberId, req)

        // then
        verify(imageRepository, never()).findAllByAccessUrlIn(any())
    }

    @Test
    @DisplayName("게시글 수정 시 새 이미지 URL도 USED로 마킹한다")
    fun updatePost_withNewImage_marksImageAsUsed() {
        // given
        val memberId = 1L
        val postId = 1L
        val newContent = "<img src=\"http://minio/bucket/new.jpg\" />"

        val member = Member.builder().build()
        ReflectionTestUtils.setField(member, "id", memberId)
        val existingPost = Post.builder().title("기존").content("기존").member(member).build()

        whenever(postRepository.findById(postId)).thenReturn(Optional.of(existingPost))

        val mockImage = mock<Image>()
        whenever(imageRepository.findAllByAccessUrlIn(any())).thenReturn(listOf(mockImage))

        // when
        postService.updatePost(memberId, postId, PostUpdateReq("수정 제목", newContent, null, null))

        // then
        verify(imageRepository).findAllByAccessUrlIn(any())
        verify(mockImage).status = Image.ImageStatus.USED
        verify(mockImage).domain = Image.ImageDomain.POST
        verify(mockImage).domainId = postId
    }
}

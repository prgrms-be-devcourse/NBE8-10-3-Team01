package com.plog.domain.post.controller

import com.plog.domain.member.entity.Member
import com.plog.domain.post.dto.*
import com.plog.domain.post.entity.Post
import com.plog.domain.post.service.PostService
import com.plog.domain.hashtag.service.HashTagService
import com.plog.testUtil.SecurityTestConfig
import com.plog.testUtil.WebMvcTestSupport
import com.plog.testUtil.WithCustomMockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(PostController::class)
@Import(SecurityTestConfig::class)
@ActiveProfiles("test")
class PostControllerTest : WebMvcTestSupport() {

    @MockitoBean
    lateinit var postService: PostService

    @MockitoBean
    lateinit var hashTagService: HashTagService

    @Test
    @DisplayName("게시글 생성 시 인증된 사용자가 요청하면 성공한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun createPostSuccess() {
        // [Given]
        val mockMemberId = 1L
        val createdPostId = 100L

        val request = PostCreateReq("게시글 제목", "게시글 본문", null, "example.com")

        whenever(postService.createPost(eq(mockMemberId), any()))
            .thenReturn(createdPostId)

        mockMvc.perform(post("/api/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/posts/$createdPostId"))
    }

    @Test
    @DisplayName("게시글 생성 시 제목이 비어있으면 400 에러를 반환한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun createPostWithEmptyTitle() {
        // [Given]
        val request = PostCreateReq("", "게시글 본문", null, null)

        // [When]
        val resultActions = mockMvc.perform(post("/api/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(print())

        // [Then]
        resultActions.andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("게시글 생성 시 본문이 비어있으면 400 에러를 반환한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun createPostWithEmptyContent() {
        // [Given]
        val request = PostCreateReq("제목", "", null, null)

        // [When]
        val resultActions = mockMvc.perform(post("/api/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(print())

        // [Then]
        resultActions.andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("게시글 상세 조회 시 응답 데이터 형식을 확인한다")
    @Throws(Exception::class)
    fun getPostSuccess() {
        // [Given]
        val author = Member(email = "email", password = "password", nickname = "nickname")
        val mockPost = Post(
            title = "조회 제목",
            content = "조회 본문",
            member = author
        )

        whenever(postService.getPostDetail(any(), any(), any())).thenReturn(PostInfoRes.from(mockPost))

        // [When]
        val resultActions = mockMvc
            .perform(get("/api/posts/1"))
            .andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.title").value("조회 제목"))
            .andExpect(jsonPath("$.message").value("게시글 조회 성공"))
    }

    @Test
    @DisplayName("게시글 목록 조회 시 페이징 처리된 Slice 리스트를 반환한다")
    @Throws(Exception::class)
    fun getPostsSuccess() {
        // [Given]
        val author = Member(email = "email", password = "password", nickname = "nickname")
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val post1 = Post(title = "제목1", content = "내용1", member = author)
        val post2 = Post(title = "제목2", content = "내용2", member = author)

        val sliceResponse = SliceImpl(
            listOf(PostListRes.from(post2), PostListRes.from(post1)),
            pageable,
            false // 다음 페이지가 없다고 가정
        )

        whenever(postService.getPosts(any())).thenReturn(sliceResponse)

        // [When]
        val resultActions = mockMvc
            .perform(get("/api/posts")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
            .andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].title").value("제목2"))
            .andExpect(jsonPath("$.data.content[1].title").value("제목1"))
            .andExpect(jsonPath("$.data.number").value(0))
            .andExpect(jsonPath("$.data.last").value(true))
            .andExpect(jsonPath("$.message").value("게시글 목록 조회 성공"))
    }

    @Test
    @DisplayName("게시글 수정 요청 시 204 No Content를 반환한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun updatePostSuccess() {
        // [Given]
        val postId = 1L
        val requestDto = PostUpdateReq("수정 제목", "수정 본문", null, null)

        // [When]
        val resultActions = mockMvc.perform(
            put("/api/posts/{id}", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        ).andDo(print())

        // [Then]
        resultActions.andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("게시글 수정 시 제목이 비어있으면 400 에러를 반환한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun updatePostWithEmptyTitle() {
        // [Given]
        val postId = 1L
        val requestDto = PostUpdateReq("", "수정 본문", null, null)

        // [When]
        val resultActions = mockMvc.perform(
            put("/api/posts/{id}", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        ).andDo(print())

        // [Then]
        resultActions.andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("게시글 수정 시 본문이 비어있으면 400 에러를 반환한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun updatePostWithEmptyContent() {
        // [Given]
        val requestDto = PostUpdateReq("제목", "", null, null)

        // [When]
        val resultActions = mockMvc.perform(
            put("/api/posts/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        ).andDo(print())

        // [Then]
        resultActions.andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("게시글 삭제 요청 시 성공하면 204 No Content를 반환한다")
    @WithCustomMockUser
    @Throws(Exception::class)
    fun deletePostSuccess() {
        // [When]
        val resultActions = mockMvc.perform(
            delete("/api/posts/1")
        ).andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isNoContent)
            .andExpect(jsonPath("$").doesNotExist())

        // 서비스의 deletePost 메서드가 호출되었는지 검증합니다.
        verify(postService).deletePost(any(), any())
    }

    @Test
    @DisplayName("특정 회원의 게시글 목록 조회 시 Slice 구조와 상세 DTO 필드들이 JSON에 포함되어야 한다")
    @Throws(Exception::class)
    fun getPostsByMemberApiSuccess() {
        // [Given]
        val memberId = 1L
        val now = LocalDateTime.now()
        val pageable = PageRequest.of(0, 10) // 테스트용 페이징 정보

        // PostInfoRes 데이터 준비
        val res = PostInfoRes(
            100L, "제목", "본문", 5, now, now, null, null, null, 1L, "nickname", "imageURL"
        )

        // SliceImpl을 사용하여 서비스 반환값 모킹 (데이터 1개, 다음 페이지 없음)
        val sliceResponse = SliceImpl(listOf(res), pageable, false)

        // 서비스 메서드 호출 시 Pageable 파라미터를 포함하도록 설정
        whenever(postService.getPostsByMember(eq(memberId), any()))
            .thenReturn(sliceResponse)

        // [When]
        val resultActions = mockMvc.perform(
            get("/api/posts/members/{memberId}", memberId)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        // [Then]
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            // Slice 구조에서는 데이터가 'content' 필드 안에 배열로 들어갑니다.
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].id").value(100))
            .andExpect(jsonPath("$.data.content[0].title").value("제목"))
            .andExpect(jsonPath("$.data.content[0].content").value("본문"))
            .andExpect(jsonPath("$.data.content[0].viewCount").value(5))
            .andExpect(jsonPath("$.data.content[0].createDate").exists())
            .andExpect(jsonPath("$.data.content[0].modifyDate").exists())
            // Slice 메타데이터 검증 (다음 페이지 여부 등)
            .andExpect(jsonPath("$.data.last").value(true))
            .andExpect(jsonPath("$.data.first").value(true))
            .andExpect(jsonPath("$.message").value("사용자 게시글 목록 조회 성공"))

        // 서비스 계층으로 정확한 인자가 전달되었는지 확인합니다.
        verify(postService).getPostsByMember(eq(memberId), any())
    }
}

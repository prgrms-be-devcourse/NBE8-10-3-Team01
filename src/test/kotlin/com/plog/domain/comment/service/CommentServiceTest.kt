package com.plog.domain.comment.service

import com.plog.domain.comment.dto.CommentCreateReq
import com.plog.domain.comment.entity.Comment
import com.plog.domain.comment.entity.CommentLike
import com.plog.domain.comment.repository.CommentLikeRepository
import com.plog.domain.comment.repository.CommentRepository
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.entity.Post
import com.plog.domain.post.entity.PostStatus
import com.plog.domain.post.repository.PostRepository
import io.mockk.*
import org.junit.jupiter.api.DisplayName
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import java.time.LocalDateTime
import java.util.Optional

class CommentServiceTest {

    private val commentRepository = mockk<CommentRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val postRepository = mockk<PostRepository>()
    private val commentLikeRepository = mockk<CommentLikeRepository>()

    private val commentService: CommentService = CommentServiceImpl(
        commentRepository,
        postRepository,
        memberRepository,
        commentLikeRepository
    )

    private fun createMember(id: Long): Member {
        val member = Member(
            "test@example.com",
            "1234",
            "테스터",
            null
        )
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }

    @Test
    @DisplayName("성공적인 댓글 작성에 대한 테스트")
    fun createComment_success(){

        //GIVEN
        val commentId = 100L
        val postId = 1L
        val memberId = 10L
        val content = "새로운 루트 댓글입니다."
        val req = CommentCreateReq(content, null)

        val member = createMember(memberId)
        val post = Post(
            "테스트 게시글",
            "게시글 내용",
            "요약",
            PostStatus.PUBLISHED,
            0,
            member,
            mutableListOf(), // 비어있는 리스트
            "default_thumb.png"
        )

        val comment1 = Comment(member, post, content, null, false)

        ReflectionTestUtils.setField(comment1, "id", commentId)

        every { memberRepository.findById(memberId) } returns java.util.Optional.of(member)
        every { postRepository.findById(postId) } returns java.util.Optional.of(post)
        every { commentRepository.save(any<Comment>()) } returns comment1


        //WHEN
        val resultId = commentService.createComment(postId, memberId, req)

        //THEN
        assertThat(resultId).isEqualTo(100L)
        assertThat(comment1.content).isEqualTo(content)
        assertThat(comment1.post).isEqualTo(post)
        assertThat(comment1.id).isEqualTo(resultId)
        assertThat(comment1.parent).isNull()
        assertThat(comment1.author).isEqualTo(member)
        assertThat(comment1.deleted).isFalse()
    }

    @Test
    @DisplayName("성공적인 대댓글 작성에 대한 테스트")
    fun createReply_Success() {
        //GIVEN
        val postId = 1L
        val parentCommentId = 100L
        val memberId = 10L
        val req = CommentCreateReq("새로운 대댓글입니다.", parentCommentId)

        val member = createMember(memberId)

        val post = Post("제목", "내용", "요약", PostStatus.PUBLISHED, 0, member, mutableListOf(), "thumb.png")
        ReflectionTestUtils.setField(post, "id", postId)

        val parentComment = Comment(member, post, "부모 댓글", null, false)
        ReflectionTestUtils.setField(parentComment, "id", parentCommentId)

        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { postRepository.findById(postId) } returns Optional.of(post)
        every { commentRepository.findById(parentCommentId) } returns Optional.of(parentComment)

        val commentSlot = slot<Comment>()
        every { commentRepository.save(capture(commentSlot)) } answers {
            val savedComment = firstArg<Comment>()
            ReflectionTestUtils.setField(savedComment, "id", 200L) // DB 저장 후 ID가 생긴 것처럼 시뮬레이션
            savedComment
        }

        //WHEN
        val result = commentService.createComment(postId, memberId, req)

        //THEN
        verify(exactly = 1) { commentRepository.save(any()) }

        val capturedComment = commentSlot.captured

        assertThat(result).isNotNull
        assertThat(capturedComment.content).isEqualTo(req.content)
        assertThat(capturedComment.post.id).isEqualTo(postId)
        assertThat(capturedComment.parent?.id).isEqualTo(parentCommentId)
        assertThat(capturedComment.author.id).isEqualTo(memberId)
    }

    @Test
    @DisplayName("성공적인 댓글 수정에 대한 테스트")
    fun updateComment_success() {
        //GIVEN
        val commentId = 100L
        val memberId = 10L
        val content = "수정 전 원래 내용입니다."

        val newContent = "깔끔하게 수정된 내용입니다."

        val member = createMember(memberId)
        val post = Post(
            "테스트 게시글", "내용", "요약", PostStatus.PUBLISHED, 0, member, mutableListOf(), "thumb.png"
        )

        val comment1 = Comment(member, post, content, null, false)
        ReflectionTestUtils.setField(comment1, "id", commentId)


        every { commentRepository.findById(commentId) } returns java.util.Optional.of(comment1)

        //WHEN
        commentService.updateComment(commentId, memberId, newContent)

        //THEN
        assertThat(comment1.content).isEqualTo(newContent)
        assertThat(comment1.id).isEqualTo(commentId)
        assertThat(comment1.author.id).isEqualTo(memberId)
    }

    @Test
    @DisplayName("성공적인 댓글 삭제 테스트 (자식이 없는 댓글의 하드 삭제)")
    fun deleteComment_success_hardDelete() {
        //GIVEN
        val commentId = 100L
        val memberId = 10L
        val content = "삭제될 댓글 내용입니다."

        val member = createMember(memberId)

        val post = Post(
            "테스트 게시글",
            "내용",
            "요약",
            PostStatus.PUBLISHED,
            0,
            member,
            mutableListOf(),
            "thumb.png"
        )

        val comment = Comment(member, post, content, null, false)
        ReflectionTestUtils.setField(comment, "id", commentId)

        every { commentRepository.findById(commentId) } returns java.util.Optional.of(comment)
        every { commentRepository.delete(comment) } returns Unit
        every { commentRepository.existsByParent(any()) } returns false

        //WHEN
        commentService.deleteComment(commentId, memberId)

        // THEN
        verify(exactly = 1) { commentRepository.delete(comment) }
    }

    @Test
    @DisplayName("성공적인 댓글 삭제 테스트 (자식이 있는 댓글의 소프트 삭제)")
    fun deleteComment_success_softDelete() {
        // GIVEN
        val commentId = 100L
        val memberId = 10L
        val content = "삭제될 댓글 내용입니다."

        val member = createMember(memberId)

        val post = Post(
            "테스트 게시글",
            "내용",
            "요약",
            PostStatus.PUBLISHED,
            0,
            member,
            mutableListOf(),
            "thumb.png"
        )

        val comment = Comment(member, post, content, null, false)
        ReflectionTestUtils.setField(comment, "id", commentId)

        every { commentRepository.findById(commentId) } returns java.util.Optional.of(comment)
        every { commentRepository.existsByParent(any()) } returns true

        // WHEN
        commentService.deleteComment(commentId, memberId)

        // THEN

        verify(exactly = 0) { commentRepository.delete(any()) }

        assertThat(comment.deleted).isTrue()
        assertThat(comment.content).isEqualTo("[삭제된 댓글입니다.]")
    }

    @Test
    @DisplayName("루트 댓글 10개만 조회되는 것에 대한 테스트")
    fun getComments_Slicing_Success() {
        // GIVEN
        val postId = 1L
        val pageNumber = 0
        val pageSize = 10

        val member = createMember(1L)

        val post = Post(
            "테스트 게시글",
            "내용",
            "요약",
            PostStatus.PUBLISHED,
            0,
            member,
            mutableListOf(),
            "thumb.png"
        )

        ReflectionTestUtils.setField(post, "id", postId)

        val comments = (1..11).map { i ->
            val author = createMember(i.toLong())
            val c = Comment(author, post, "루트 댓글 $i", null, false)
            ReflectionTestUtils.setField(c, "id", i.toLong())
            ReflectionTestUtils.setField(c, "createDate", LocalDateTime.now())
            ReflectionTestUtils.setField(c, "modifyDate", LocalDateTime.now())
            c
        }

        every { postRepository.existsById(postId) } returns true

        every {
            commentRepository.findRepliesWithMemberAndImageByParentId(any(), any())
        } returns SliceImpl(emptyList(), PageRequest.of(0, 5), false)

        val mockSlice: Slice<Comment> = SliceImpl(
            comments.subList(0, 10),
            PageRequest.of(pageNumber, pageSize),
            true
        )

        every {
            commentRepository.findCommentsWithMemberAndImageByPostId(eq(postId), any())
        } returns mockSlice

        // WHEN
        val result = commentService.getCommentsByPostId(postId, pageNumber)

        // THEN
        assertThat(result.content.size).isEqualTo(10)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    @DisplayName("대댓글 5개 조회되는 것에 대한 테스트")
    fun getReplies_Slicing_Success() {
        // GIVEN
        val parentCommentId = 100L
        val pageNumber = 0
        val pageSize = 5

        val member = createMember(1L)

        val post = Post(
            "테스트 게시글",
            "내용",
            "요약",
            PostStatus.PUBLISHED,
            0,
            member,
            mutableListOf(),
            "thumb.png"
        )

        every { commentRepository.existsById(parentCommentId) } returns true

        val parentComment = Comment(member, post, "부모 댓글", null, false)
        ReflectionTestUtils.setField(parentComment, "id", parentCommentId)

        val replies = (1..6).map { i ->
            val author = createMember(i.toLong())
            val r = Comment(author, post, "대댓글 $i", parentComment, false)
            ReflectionTestUtils.setField(r, "id", i.toLong() + 1000)
            ReflectionTestUtils.setField(r, "createDate", LocalDateTime.now())
            ReflectionTestUtils.setField(r, "modifyDate", LocalDateTime.now())
            r
        }


        val mockReplySlice: Slice<Comment> = SliceImpl(
            replies.subList(0, 5),
            PageRequest.of(pageNumber, pageSize),
            true
        )

        every {
            commentRepository.findRepliesWithMemberAndImageByParentId(eq(parentCommentId), any())
        } returns mockReplySlice

        // WHEN
        val result = commentService.getRepliesByCommentId(parentCommentId, pageNumber)

        // THEN
        assertThat(result.content.size).isEqualTo(5)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    @DisplayName("좋아요가 없는 상태에서 호출하면 새로운 좋아요가 저장되고 true를 반환한다")
    fun toggleLike_CreateSuccess() {
        // given
        val memberId = 1L
        val commentId = 100L
        val member = createMember(memberId)
        val post = Post(
            "테스트 게시글", "게시글 내용", "요약", PostStatus.PUBLISHED, 0, member, mutableListOf(), "default_thumb.png"
        )
        val comment = Comment(member, post, "댓글", null, false)

        every { memberRepository.getReferenceById(memberId) } returns member
        every { commentRepository.findByIdWithLock(commentId) } returns Optional.of(comment)
        every { commentLikeRepository.findByCommentIdAndMemberId(commentId, memberId) } returns null
        every { commentLikeRepository.save(any<CommentLike>()) } returns mockk()
        every { commentRepository.incrementLikeCount(commentId) } returns 1

        // when
        val result = commentService.toggleCommentLike(commentId, memberId)

        // then
        verify(exactly = 1) { commentLikeRepository.save(any<CommentLike>()) }
        verify(exactly = 1) { commentRepository.incrementLikeCount(commentId) }
        assertTrue(result)
    }

    @Test
    @DisplayName("이미 좋아요가 있는 상태에서 호출하면 기존 좋아요가 삭제되고 false를 반환한다")
    fun toggleLike_DeleteSuccess() {
        // given
        val memberId = 1L
        val commentId = 100L
        val member = createMember(memberId)
        val post = Post(
            "테스트 게시글", "게시글 내용", "요약", PostStatus.PUBLISHED, 0, member, mutableListOf(), "default_thumb.png"
        )
        val comment = Comment(member, post, "댓글", null, false)
        val existingLike = CommentLike(member = member, comment = comment)

        every { memberRepository.getReferenceById(memberId) } returns member
        every { commentRepository.findByIdWithLock(commentId) } returns Optional.of(comment)
        every { commentLikeRepository.findByCommentIdAndMemberId(commentId, memberId) } returns existingLike
        every { commentLikeRepository.delete(any<CommentLike>()) } just Runs
        every { commentRepository.decrementLikeCount(commentId) } returns 1

        // when
        val result = commentService.toggleCommentLike(commentId, memberId)

        // then
        verify(exactly = 1) { commentLikeRepository.delete(any<CommentLike>()) }
        verify(exactly = 1) { commentRepository.decrementLikeCount(commentId) }
        assertFalse(result)
    }

}

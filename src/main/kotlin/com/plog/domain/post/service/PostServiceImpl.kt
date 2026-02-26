package com.plog.domain.post.service

import com.plog.domain.comment.constant.CommentConstants
import com.plog.domain.comment.dto.CommentInfoRes
import com.plog.domain.comment.dto.ReplyInfoRes
import com.plog.domain.comment.entity.Comment
import com.plog.domain.comment.repository.CommentRepository
import com.plog.domain.hashtag.entity.HashTag
import com.plog.domain.hashtag.entity.PostHashTag
import com.plog.domain.hashtag.repository.HashTagRepository
import com.plog.domain.hashtag.repository.PostHashTagRepository
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.dto.PostCreateReq
import com.plog.domain.post.dto.PostInfoRes
import com.plog.domain.post.dto.PostListRes
import com.plog.domain.post.dto.PostUpdateReq
import com.plog.domain.post.entity.Post
import com.plog.domain.post.entity.PostStatus
import com.plog.domain.post.repository.PostRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.errorCode.PostErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.exception.exceptions.PostException
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository

/**
 * [PostService] 인터페이스의 기본 구현체입니다.
 * <p>
 * `@Service`와 `@Transactional`을 통해 스프링 빈으로 관리되며,
 * CommonMark 라이브러리를 이용한 마크다운 파싱 로직을 포함합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * CommonMark v0.21.0 (Parser, TextContentRenderer)
 *
 * @author MintyU
 * @since 2026-01-19
 */
@Service
@Transactional(readOnly = true)
class PostServiceImpl(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val memberRepository: MemberRepository,
    private val postHashTagRepository: PostHashTagRepository,
    private val hashTagRepository: HashTagRepository,
    private val imageRepository: ImageRepository
) : PostService {

    companion object {
        /** 요약본 생성을 위한 최대 글자 수 기준입니다. */
        private const val MAX_SUMMARY_LENGTH = 150
    }

    @Transactional
    override fun createPost(memberId: Long, req: PostCreateReq): Long {
        val member = memberRepository.getReferenceById(memberId)
        val plainText = extractPlainText(req.content)
        val summary = extractSummary(plainText)

        val post = Post.builder()
            .title(req.title)
            .content(req.content)
            .summary(summary)
            .member(member)
            .status(PostStatus.PUBLISHED)
            .thumbnail(req.thumbnail)
            .build()
        val savedPost = postRepository.save(post)

        applyTags(savedPost, req.hashtags)

        updateImageStatusUsed(req.content, req.thumbnail, savedPost.id!!)

        return savedPost.id ?: throw IllegalStateException("Post ID must not be null after save")
    }

    @Transactional
    override fun getPostDetail(id: Long, pageNumber: Int): PostInfoRes {
        val post = postRepository.findByIdWithMember(id)
            .orElseThrow {
                PostException(
                    PostErrorCode.POST_NOT_FOUND,
                    "[PostServiceImpl#getPostDetail] can't find post by id", "존재하지 않는 게시물입니다."
                )
            }

        post.incrementViewCount()

        val pageable = PageRequest.of(
            pageNumber,
            CommentConstants.COMMENT_PAGE_SIZE,
            Sort.by(Sort.Direction.ASC, CommentConstants.DEFAULT_SORT_FIELD)
        )

        val comments = commentRepository.findCommentsWithMemberAndImageByPostId(id, pageable)

        val commentResSlice = comments.map { convertToCommentInfoRes(it) }

        return PostInfoRes.from(post, commentResSlice)
    }

    private fun convertToCommentInfoRes(comment: Comment): CommentInfoRes {
        val replyPageable = PageRequest.of(
            0,
            CommentConstants.REPLY_PAGE_SIZE,
            Sort.by("createDate").ascending()
        )

        val replySlice = commentRepository.findRepliesWithMemberAndImageByParentId(comment.id!!, replyPageable)

        return CommentInfoRes.from(comment, replySlice.map { ReplyInfoRes.from(it) })
    }

    @Transactional(readOnly = true)
    override fun getPosts(pageable: Pageable): Slice<PostListRes> {
        return postRepository.findAllWithMember(pageable)
            .map { PostListRes.from(it) }
    }

    @Transactional
    override fun updatePost(memberId: Long, postId: Long, req: PostUpdateReq) {
        val post = postRepository.findById(postId)
            .orElseThrow {
                PostException(
                    PostErrorCode.POST_NOT_FOUND,
                    "[PostServiceImpl#updatePost] can't find post", "존재하지 않는 게시물입니다."
                )
            }

        if (post.member.id != memberId) {
            throw AuthException(
                AuthErrorCode.USER_AUTH_FAIL,
                "[PostServiceImpl#updatePost] user $memberId is not the owner of post $postId",
                "해당 게시물을 수정할 권한이 없습니다."
            )
        }

        val plainText = extractPlainText(req.content)
        val summary = extractSummary(plainText)

        post.update(req.title, req.content, summary, req.thumbnail)

        postHashTagRepository.deleteAllByPostId(postId)

        applyTags(post, req.hashtags)

        updateImageStatusUsed(req.content, req.thumbnail, postId)
    }

    @Transactional
    override fun deletePost(memberId: Long, postId: Long) {
        // 1. 게시물 존재 여부 확인 및 조회
        val post = postRepository.findById(postId)
            .orElseThrow {
                PostException(
                    PostErrorCode.POST_NOT_FOUND,
                    "[PostServiceImpl#deletePost] can't find post by id", "존재하지 않는 게시물입니다."
                )
            }

        // 2. 작성자 본인 확인 (권한 체크)
        if (post.member.id != memberId) {
            throw AuthException(
                AuthErrorCode.USER_AUTH_FAIL,
                "[PostServiceImpl#deletePost] user $memberId is not the owner of post $postId",
                "해당 게시물을 삭제할 권한이 없습니다."
            )
        }

        // 3. 제약 조건 위반을 방지하기 위해 자식(대댓글)부터 삭제
        commentRepository.deleteRepliesByPostId(postId)
        // 4. 그 후 부모 댓글 삭제
        commentRepository.deleteParentsByPostId(postId)
        // 5. 연결된 해시태그 정보 삭제
        postHashTagRepository.deleteAllByPostId(postId)
        // 6. 게시물 삭제
        postRepository.delete(post)
    }

    @Transactional(readOnly = true)
    override fun getPostsByMember(memberId: Long, pageable: Pageable): Slice<PostInfoRes> {
        val postSlice = postRepository.findAllByMemberId(memberId, pageable)

        return postSlice.map { PostInfoRes.from(it) }
    }

    /**
     * 마크다운 텍스트에서 특수기호를 제거하고 순수 텍스트만 추출합니다.
     * @param markdown 마크다운 원문
     * @return 추출된 순수 텍스트
     */
    private fun extractPlainText(markdown: String): String {
        val parser = Parser.builder().build()
        val document = parser.parse(markdown)
        val renderer = TextContentRenderer.builder().build()
        return renderer.render(document)
    }

    /**
     * 순수 텍스트에서 앞부분 150자만 추출하여 요약글을 생성하며,
     * 글자 수를 초과할 경우 "..."을 접미사로 추가합니다.
     * @param plainText 추출된 순수 텍스트
     * @return 가공된 요약본 문자열
     */
    private fun extractSummary(plainText: String): String =
        if (plainText.length <= MAX_SUMMARY_LENGTH) {
            plainText
        } else {
            plainText.take(MAX_SUMMARY_LENGTH) + "..."
        }

    private fun applyTags(post: Post, tagNames: List<String>?) {
        if (tagNames.isNullOrEmpty()) return

        tagNames.forEach { rawName ->
            val normalizedName = normalizeTag(rawName)

            val hashTag = hashTagRepository.findByName(normalizedName)
                ?: hashTagRepository.save(HashTag(normalizedName))

            val postId = post.id ?: throw IllegalStateException("Post ID must not be null")
            val hashTagId = hashTag.id ?: throw IllegalStateException("HashTag ID must not be null")

            if (!postHashTagRepository.existsByPostIdAndHashTagId(postId, hashTagId)) {
                val postHashTag = PostHashTag.builder()
                    .post(post)
                    .hashTag(hashTag)
                    .displayName(rawName)
                    .build()

                postHashTagRepository.save(postHashTag)
            }
        }
    }

    /**
     * 게시글의 content/thumbnail에서 이미지 URL 추출하여 상태를 USED로 업데이트
     */
    private fun updateImageStatusUsed(content: String, thumbnail: String?, postId: Long) {
        val imageUrls = extractImageUrls(content) + listOfNotNull(thumbnail)
        if (imageUrls.isEmpty()) return

        val images = imageRepository.findAllByAccessUrlIn(imageUrls)
        images.forEach { image ->
            image.status = "USED"
            image.domain = "POST"
            image.domainId = postId
        }
    }

    private fun extractImageUrls(markdown: String): List<String> {
        // <img src="..." /> 또는 markdown ![alt](url) 패턴
        val imgRegex = Regex("""src=["']([^"']+)["']""")
        val mdRegex = Regex("""!\[.*?\]\(([^)]+)\)""")

        return (imgRegex.findAll(markdown) + mdRegex.findAll(markdown))
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    private fun normalizeTag(name: String): String {
        return name.trim().lowercase().replace(" ", "_")
    }

}

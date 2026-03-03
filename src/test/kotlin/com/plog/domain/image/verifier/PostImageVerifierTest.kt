package com.plog.domain.image.verifier

import com.plog.domain.image.entity.Image
import com.plog.domain.member.entity.Member
import com.plog.domain.post.entity.Post
import com.plog.domain.post.repository.PostRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class PostImageVerifierTest {

    private lateinit var postRepository: PostRepository
    private lateinit var verifier: PostImageVerifier

    private val member = Member(email = "test@test.com", password = "pw", nickname = "nick")
    private val accessUrl = "http://example.com/post/image/1/uuid.jpg"

    @BeforeEach
    fun setUp() {
        postRepository = mockk()
        verifier = PostImageVerifier(postRepository)
    }

    @Test
    fun `supports는 POST 도메인만 true`() {
        assertTrue(verifier.supports("POST"))
        assertFalse(verifier.supports("PROFILE"))
        assertFalse(verifier.supports("OTHER"))
    }

    @Test
    fun `post content에 URL이 포함되면 사용 중으로 판단`() {
        // given
        val image = makeImage(domainId = 1L, accessUrl = accessUrl)
        val post = Post(title = "제목", content = "본문에 $accessUrl 이 있음", member = member)
        every { postRepository.findById(1L) } returns Optional.of(post)

        // when & then
        assertTrue(verifier.isInUse(image))
    }

    @Test
    fun `post thumbnail이 URL과 일치하면 사용 중으로 판단`() {
        // given
        val image = makeImage(domainId = 1L, accessUrl = accessUrl)
        val post = Post(title = "제목", content = "본문", member = member, thumbnail = accessUrl)
        every { postRepository.findById(1L) } returns Optional.of(post)

        // when & then
        assertTrue(verifier.isInUse(image))
    }

    @Test
    fun `post content와 thumbnail 모두 URL 없으면 사용 안 함으로 판단`() {
        // given
        val image = makeImage(domainId = 1L, accessUrl = accessUrl)
        val post = Post(title = "제목", content = "URL 없는 본문", member = member, thumbnail = null)
        every { postRepository.findById(1L) } returns Optional.of(post)

        // when & then
        assertFalse(verifier.isInUse(image))
    }

    @Test
    fun `domainId가 null이면 사용 안 함으로 판단`() {
        // given
        val image = makeImage(domainId = null, accessUrl = accessUrl)

        // when & then
        assertFalse(verifier.isInUse(image))
    }

    @Test
    fun `게시글을 찾을 수 없으면 사용 안 함으로 판단`() {
        // given
        val image = makeImage(domainId = 999L, accessUrl = accessUrl)
        every { postRepository.findById(999L) } returns Optional.empty()

        // when & then
        assertFalse(verifier.isInUse(image))
    }

    private fun makeImage(domainId: Long?, accessUrl: String) = Image(
        originalName = "test.jpg",
        storedName = "post/image/uuid.jpg",
        accessUrl = accessUrl,
        uploader = null,
        domain = Image.ImageDomain.POST,
        status = Image.ImageStatus.USED,
        domainId = domainId
    )
}

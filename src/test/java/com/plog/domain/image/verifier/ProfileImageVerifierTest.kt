package com.plog.domain.image.verifier

import com.plog.domain.image.entity.Image
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class ProfileImageVerifierTest {

    private lateinit var memberRepository: MemberRepository
    private lateinit var verifier: ProfileImageVerifier

    private val accessUrl = "http://example.com/profile/image/1/uuid.jpg"

    @BeforeEach
    fun setUp() {
        memberRepository = mockk()
        verifier = ProfileImageVerifier(memberRepository)
    }

    @Test
    fun `supports는 PROFILE 도메인만 true`() {
        assertTrue(verifier.supports("PROFILE"))
        assertFalse(verifier.supports("POST"))
        assertFalse(verifier.supports("OTHER"))
    }

    @Test
    fun `member의 profileImage URL과 일치하면 사용 중으로 판단`() {
        // given
        val image = makeImage(domainId = 1L, accessUrl = accessUrl)
        val profileImage = makeImage(domainId = 1L, accessUrl = accessUrl)
        val member = Member(email = "test@test.com", password = "pw", nickname = "nick", profileImage = profileImage)
        every { memberRepository.findById(1L) } returns Optional.of(member)

        // when & then
        assertTrue(verifier.isInUse(image))
    }

    @Test
    fun `member의 profileImage URL이 다르면 사용 안 함으로 판단`() {
        // given
        val image = makeImage(domainId = 1L, accessUrl = accessUrl)
        val otherImage = makeImage(domainId = 1L, accessUrl = "http://example.com/other.jpg")
        val member = Member(email = "test@test.com", password = "pw", nickname = "nick", profileImage = otherImage)
        every { memberRepository.findById(1L) } returns Optional.of(member)

        // when & then
        assertFalse(verifier.isInUse(image))
    }

    @Test
    fun `member의 profileImage가 null이면 사용 안 함으로 판단`() {
        // given
        val image = makeImage(domainId = 1L, accessUrl = accessUrl)
        val member = Member(email = "test@test.com", password = "pw", nickname = "nick", profileImage = null)
        every { memberRepository.findById(1L) } returns Optional.of(member)

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
    fun `회원을 찾을 수 없으면 사용 안 함으로 판단`() {
        // given
        val image = makeImage(domainId = 999L, accessUrl = accessUrl)
        every { memberRepository.findById(999L) } returns Optional.empty()

        // when & then
        assertFalse(verifier.isInUse(image))
    }

    private fun makeImage(domainId: Long?, accessUrl: String) = Image.builder()
        .originalName("profile.jpg")
        .storedName("profile/image/uuid.jpg")
        .accessUrl(accessUrl)
        .uploader(null)
        .domain(Image.ImageDomain.PROFILE)
        .status(Image.ImageStatus.USED)
        .domainId(domainId)
        .build()
}

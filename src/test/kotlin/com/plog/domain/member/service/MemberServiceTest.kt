package com.plog.domain.member.service

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.domain.member.dto.MemberUpdaterReq
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

/**
 * [MemberService] 에 대한 테스트코드입니다.
 */
@ExtendWith(MockitoExtension::class)
class MemberServiceTest {

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var member: Member

    @InjectMocks
    lateinit var memberService: MemberServiceImpl

    @Test
    fun findMemberWithId_success() {
        //given
        val userId = 1L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        whenever(member.id).thenReturn(userId)
        whenever(member.email).thenReturn("example@email.com")
        whenever(member.nickname).thenReturn("jack")
        whenever(member.createDate).thenReturn(LocalDateTime.now())

        //when
        val response = memberService.findMemberWithId(userId)

        //then
        assertThat(response.id).isEqualTo(userId)
    }

    @Test
    fun findMemberWithId_fail_userNotFound() {
        //given
        val userId = 1L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        //when
        val ex = assertThrows(AuthException::class.java) {
            memberService.findMemberWithId(userId)
        }

        //then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        assertThat(ex.logMessage).contains("can't find user by id")
    }

    @Test
    fun findMemberWithNickname_success() {
        // given
        val nickname = "jack"
        whenever(memberRepository.findByNickname(nickname)).thenReturn(member)
        whenever(member.id).thenReturn(1L)
        whenever(member.nickname).thenReturn(nickname)
        whenever(member.email).thenReturn("example@email.com")
        whenever(member.createDate).thenReturn(LocalDateTime.now())

        // when
        val response = memberService.findMemberWithNickname(nickname)

        // then
        assertThat(response.id).isEqualTo(1L)
        verify(memberRepository, times(1)).findByNickname(nickname)
    }

    @Test
    fun findMemberWithNickname_fail_userNotFound() {
        // given
        val nickname = "jack"
        whenever(memberRepository.findByNickname(nickname)).thenReturn(null)

        // when
        val ex = assertThrows(AuthException::class.java) {
            memberService.findMemberWithNickname(nickname)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        assertThat(ex.logMessage).contains("can't find user by nickname")
        verify(memberRepository, times(1)).findByNickname(nickname)
    }

    @Test
    fun updateMemberInfo_success() {
        // given
        val memberId = 1L
        val dto = MemberUpdaterReq("newNick")

        // 조회는 기존 member로
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))

        // update는 "업데이트된 member"를 리턴한다고 가정
        val updatedMember = mock<Member>()
        whenever(member.update(dto.nickname)).thenReturn(updatedMember)
        whenever(updatedMember.nickname).thenReturn("newNick")
        whenever(updatedMember.email).thenReturn("example@email.com")
        whenever(updatedMember.createDate).thenReturn(LocalDateTime.now())

        // save는 보통 void or 반환(member)인데 둘 다 대응 가능
        whenever(memberRepository.save(updatedMember)).thenReturn(updatedMember)

        // DTO 변환에서 id를 검증할 거면 updatedMember.id가 필요
        whenever(updatedMember.id).thenReturn(memberId)

        // when
        val response = memberService.updateMemberInfo(memberId, dto)

        // then
        assertThat(response.id).isEqualTo(memberId)

        verify(memberRepository, times(1)).findById(memberId)
        verify(member, times(1)).update("newNick")
        verify(memberRepository, times(1)).save(updatedMember)
    }

    @Test
    fun updateMemberInfo_fail_userNotFound() {
        // given
        val memberId = 1L
        val dto = MemberUpdaterReq("newNick")
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.empty())

        // when
        val ex = assertThrows(AuthException::class.java) {
            memberService.updateMemberInfo(memberId, dto)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        assertThat(ex.logMessage).contains("can't find user by id")

        verify(memberRepository, times(1)).findById(memberId)
        verify(memberRepository, never()).save(any())
        verifyNoInteractions(member) // member는 조회 자체가 안 됐으니 상호작용 없어야 정상
    }

    @Test
    fun isDuplicateEmail_true() {
        // given
        val email = "example@email.com"
        whenever(memberRepository.existsByEmail(email)).thenReturn(true)

        // when
        val result = memberService.isDuplicateEmail(email)

        // then
        assertThat(result).isTrue()
        verify(memberRepository, times(1)).existsByEmail(email)
    }

    @Test
    fun isDuplicateEmail_false() {
        // given
        val email = "example@email.com"
        whenever(memberRepository.existsByEmail(email)).thenReturn(false)

        // when
        val result = memberService.isDuplicateEmail(email)

        // then
        assertThat(result).isFalse()
        verify(memberRepository, times(1)).existsByEmail(email)
    }

    @Test
    fun isDuplicateNickname_true() {
        // given
        val nickname = "jack"
        whenever(memberRepository.existsByNickname(nickname)).thenReturn(true)

        // when
        val result = memberService.isDuplicateNickname(nickname)

        // then
        assertThat(result).isTrue()
        verify(memberRepository, times(1)).existsByNickname(nickname)
    }

    @Test
    fun isDuplicateNickname_false() {
        // given
        val nickname = "jack"
        whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)

        // when
        val result = memberService.isDuplicateNickname(nickname)

        // then
        assertThat(result).isFalse()
        verify(memberRepository, times(1)).existsByNickname(nickname)
    }

}

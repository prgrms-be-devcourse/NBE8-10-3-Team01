package com.plog.domain.member.controller

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.domain.member.service.MemberService
import com.plog.global.response.CommonResponse
import com.plog.testUtil.JsonFieldMatcher.Companion.hasKey
import com.plog.testUtil.WebMvcTestSupport
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

/**
 * [MemberController] 에 대한 슬라이스 테스트 입니다.
 */
@WebMvcTest(MemberController::class)
class MemberControllerTest : WebMvcTestSupport() {

    @MockitoBean
    lateinit var memberService: MemberService

    @Test
    @Throws(Exception::class)
    fun findMemberWithId_success() {
        //given
        val userId = 3L

        val res = MemberInfoRes(
            id = userId,
            email = "example@email.com",
            nickname = "nickname",
            createDate = LocalDateTime.of(2026, 1, 1, 0, 0)
        )
        val expected = CommonResponse.success(res)
        whenever(memberService.findMemberWithId(userId)).thenReturn(res)

        //when
        val result = mockMvc.perform(get("/api/members/id/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON))

        //then
        result.andExpect(status().isOk)
            .andExpect(hasKey(expected))
    }

    @Test
    @Throws(Exception::class)
    fun findMemberWithNickname_success() {
        //given
        val nickname = "nickname"

        val res = MemberInfoRes(
            id = 1L,
            email = "example@email.com",
            nickname = nickname,
            createDate = LocalDateTime.of(2026, 1, 1, 0, 0)
        )
        val expected = CommonResponse.success(res)
        whenever(memberService.findMemberWithNickname(nickname)).thenReturn(res)

        //when
        val result = mockMvc.perform(get("/api/members/nickname/{nickname}", nickname)
            .contentType(MediaType.APPLICATION_JSON))

        //then
        result.andExpect(status().isOk)
            .andExpect(hasKey(expected))
    }

    @Test
    @Throws(Exception::class)
    fun checkDuplicateEmail_true() {
        // given
        val email = "example@email.com"
        val duplicated = true

        val expected = CommonResponse.success(duplicated)
        whenever(memberService.isDuplicateEmail(email)).thenReturn(duplicated)

        // when
        val result = mockMvc.perform(
            get("/api/members/check/email")
                .param("email", email)
                .contentType(MediaType.APPLICATION_JSON)
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(hasKey(expected))
    }

    @Test
    @Throws(Exception::class)
    fun checkDuplicateEmail_false() {
        // given
        val email = "example@email.com"
        val duplicated = false

        val expected = CommonResponse.success(duplicated)
        whenever(memberService.isDuplicateEmail(email)).thenReturn(duplicated)

        // when
        val result = mockMvc.perform(
            get("/api/members/check/email")
                .param("email", email)
                .contentType(MediaType.APPLICATION_JSON)
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(hasKey(expected))
    }

    @Test
    @Throws(Exception::class)
    fun checkDuplicateNickname_true() {
        // given
        val nickname = "nickname"
        val duplicated = true

        val expected = CommonResponse.success(duplicated)
        whenever(memberService.isDuplicateNickname(nickname)).thenReturn(duplicated)

        // when
        val result = mockMvc.perform(
            get("/api/members/check/nickname")
                .param("nickname", nickname)
                .contentType(MediaType.APPLICATION_JSON)
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(hasKey(expected))
    }

    @Test
    @Throws(Exception::class)
    fun checkDuplicateNickname_false() {
        // given
        val nickname = "nickname"
        val duplicated = false

        val expected = CommonResponse.success(duplicated)
        whenever(memberService.isDuplicateNickname(nickname)).thenReturn(duplicated)

        // when
        val result = mockMvc.perform(
            get("/api/members/check/nickname")
                .param("nickname", nickname)
                .contentType(MediaType.APPLICATION_JSON)
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(hasKey(expected))
    }
}

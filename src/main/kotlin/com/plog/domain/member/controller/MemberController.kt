package com.plog.domain.member.controller

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.domain.member.dto.MemberUpdaterReq
import com.plog.domain.member.service.MemberService
import com.plog.global.response.CommonResponse
import com.plog.global.response.Response
import com.plog.global.security.SecurityUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * Member 도메인에 대한 controller.
 *
 * @author jack8
 * @since 2026-01-19
 */
@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberService: MemberService
) {

    @GetMapping("/id/{id}")
    fun findMemberWithId(@PathVariable("id") id: Long): ResponseEntity<Response<MemberInfoRes>> {
        val response = memberService.findMemberWithId(id)

        return ResponseEntity.ok(CommonResponse.success(response))
    }

    @GetMapping("/nickname/{nickname}")
    fun findMemberWithNickname(@PathVariable("nickname") nickname: String): ResponseEntity<Response<MemberInfoRes>> {
        val response = memberService.findMemberWithNickname(nickname)

        return ResponseEntity.ok(CommonResponse.success(response))
    }

    @PutMapping("/update")
    fun updateMember(
        @RequestBody request: MemberUpdaterReq,
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<Response<MemberInfoRes>> {
        val response = memberService.updateMemberInfo(securityUser.id, request)

        return ResponseEntity.ok(CommonResponse.success(response))
    }

    @GetMapping("/check/email")
    fun checkDuplicateEmail(@RequestParam(name = "email") email: String): ResponseEntity<Response<Boolean>> {
        val response = memberService.isDuplicateEmail(email)

        return ResponseEntity.ok(CommonResponse.success(response))
    }

    @GetMapping("/check/nickname")
    fun checkDuplicateNickname(@RequestParam(name = "nickname") nickname: String): ResponseEntity<Response<Boolean>> {
        val response = memberService.isDuplicateNickname(nickname)

        return ResponseEntity.ok(CommonResponse.success(response))
    }
}

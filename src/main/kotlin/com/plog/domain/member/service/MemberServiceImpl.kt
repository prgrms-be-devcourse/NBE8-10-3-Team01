package com.plog.domain.member.service

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.domain.member.dto.MemberUpdaterReq
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberService 에 대한 구현 클래스입니다. 실제 비지니스 로직을 정의합니다.
 *
 * **상속 정보:**
 * `MemberService` 의 자식 구현 클래스
 *
 * @author jack8
 * @see MemberService
 * @since 2026-01-18
 */
@Service
class MemberServiceImpl(
    private val memberRepository: MemberRepository
) : MemberService {

    @Transactional(readOnly = true)
    override fun findMemberWithId(id: Long): MemberInfoRes {
        val member = memberRepository.findById(id)
            .orElseThrow {
                AuthException(
                    AuthErrorCode.USER_NOT_FOUND,
                    "[MemberServiceImpl#findMemberWithId] can't find user by id",
                    "존재하지 않는 사용자입니다."
                )
            }

        return MemberInfoRes.from(member)
    }

    @Transactional(readOnly = true)
    override fun findMemberWithNickname(nickname: String): MemberInfoRes {
        val member = memberRepository.findByNickname(nickname)
            .orElseThrow {
                AuthException(
                    AuthErrorCode.USER_NOT_FOUND,
                    "[MemberServiceImpl#findMemberWithNickname] can't find user by nickname",
                    "존재하지 않는 사용자입니다."
                )
            }

        return MemberInfoRes.from(member)
    }

    @Transactional
    override fun updateMemberInfo(memberId: Long, dto: MemberUpdaterReq): MemberInfoRes {
        var member = memberRepository.findById(memberId)
            .orElseThrow {
                AuthException(
                    AuthErrorCode.USER_NOT_FOUND,
                    "[MemberServiceImpl#updateMemberInfo] can't find user by id",
                    "존재하지 않는 사용자입니다."
                )
            }

        member = member.update(dto.nickname)

        //코드의 흐름 상, save 를 일부로 명시하는 편을 좋아합니다.
        memberRepository.save(member)

        return MemberInfoRes.from(member)
    }

    @Transactional(readOnly = true)
    override fun isDuplicateEmail(email: String): Boolean {
        return memberRepository.existsByEmail(email)
    }

    @Transactional(readOnly = true)
    override fun isDuplicateNickname(nickname: String): Boolean {
        return memberRepository.existsByNickname(nickname)
    }
}

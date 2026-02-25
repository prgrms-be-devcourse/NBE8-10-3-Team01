package com.plog.domain.member.service

import com.plog.domain.member.dto.MemberInfoRes
import com.plog.domain.member.dto.MemberUpdaterReq

/**
 * 사용자에 대한 비지니스 로직을 정의한 service 레이어의 인터페이스입니다.
 *
 * 메서드를 정의하고, 해당 메서드에 대한 명세를 주석으로 작성합니다.
 *
 * **상속 정보:**
 * MemberServiceImpl 의 부모 인터페이스
 *
 * @author jack8
 * @see MemberServiceImpl
 * @since 2026-01-18
 */
interface MemberService {

    /**
     * 회원 ID를 기준으로 사용자 정보를 조회합니다.
     *
     * @param id 조회할 회원의 고유 식별자
     * @return 조회된 회원 정보
     * @throws IllegalArgumentException id가 null 인 경우
     * @throws com.plog.global.exception.exceptions.AuthException
     *         해당 ID에 대한 회원이 존재하지 않는 경우
     */
    fun findMemberWithId(id: Long): MemberInfoRes

    /**
     * 닉네임을 기준으로 사용자 정보를 조회합니다.
     *
     * @param nickname 조회할 회원의 닉네임
     * @return 조회된 회원 정보
     * @throws IllegalArgumentException nickname이 null 이거나 빈 값인 경우
     * @throws com.plog.global.exception.exceptions.AuthException
     *         해당 닉네임에 대한 회원이 존재하지 않는 경우
     */
    fun findMemberWithNickname(nickname: String): MemberInfoRes

    /**
     * 회원 정보를 수정합니다.
     *
     * 수정 대상은 회원 기본 정보이며,
     * [MemberUpdaterReq]에 포함된 값만 변경됩니다.
     *
     * @param memberId 수정할 회원의 고유 식별자
     * @param dto 수정할 회원 정보
     * @return 수정된 회원 정보
     * @throws IllegalArgumentException memberId 또는 dto가 null 인 경우
     * @throws com.plog.global.exception.exceptions.AuthException
     *         해당 회원이 존재하지 않는 경우
     */
    fun updateMemberInfo(memberId: Long, dto: MemberUpdaterReq): MemberInfoRes

    /**
     * 이메일 중복 여부를 확인합니다.
     *
     * 입력된 이메일이 이미 다른 회원에 의해
     * 사용 중인 경우 `true`를 반환합니다.
     *
     * @param email 중복 여부를 확인할 이메일
     * @return 이미 사용 중인 이메일이면 `true`, 아니면 `false`
     * @throws IllegalArgumentException email 이 null 이거나 빈 값인 경우
     */
    fun isDuplicateEmail(email: String): Boolean

    /**
     * 닉네임 중복 여부를 확인합니다.
     *
     * 입력된 닉네임이 이미 다른 회원에 의해
     * 사용 중인 경우 `true`를 반환합니다.
     *
     * @param nickname 중복 여부를 확인할 닉네임
     * @return 이미 사용 중인 닉네임이면 `true`, 아니면 `false`
     * @throws IllegalArgumentException nickname 이 null 이거나 빈 값인 경우
     */
    fun isDuplicateNickname(nickname: String): Boolean
}

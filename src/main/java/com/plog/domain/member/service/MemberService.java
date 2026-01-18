package com.plog.domain.member.service;

import com.plog.domain.member.dto.FindMemberRes;

/**
 * 사용자에 대한 비지니스 로직을 정의한 service 레이어의 인터페이스입니다.
 * <p>
 * 메서드를 정의하고, 해당 메서드에 대한 명세를 주석으로 작성합니다.
 *
 * <p><b>상속 정보:</b><br>
 * MemberServiceImpl 의 부모 인터페이스
 *
 * @author jack8
 * @see MemberServiceImpl
 * @since 2026-01-18
 */
public interface MemberService {

    FindMemberRes findMemberWithId(Long id);

    FindMemberRes findMemberWithNickname(String nickname);
}

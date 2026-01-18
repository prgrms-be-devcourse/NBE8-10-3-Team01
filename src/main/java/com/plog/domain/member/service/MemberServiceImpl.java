package com.plog.domain.member.service;

import com.plog.domain.member.dto.FindMemberRes;
import com.plog.domain.member.entity.Member;
import com.plog.domain.member.repository.MemberRepository;
import com.plog.global.exception.errorCode.AuthErrorCode;
import com.plog.global.exception.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * MemberService 에 대한 구현 클래스입니다. 실제 비지니스 로직을 정의합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@code MemberService} 의 자식 구현 클래스
 *
 * @author jack8
 * @see MemberService
 * @since 2026-01-18
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public FindMemberRes findMemberWithId(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
                        "[MemberServiceImpl#findMemberWithId] can't find user by id",
                        "존재하지 않는 사용자입니다."));

        return toDto(member);
    }

    @Override
    public FindMemberRes findMemberWithNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND,
                        "[MemberServiceImpl#findMemberWithNickname] can't find user by nickname",
                        "존재하지 않는 사용자입니다."));

        return toDto(member);
    }

    private FindMemberRes toDto(Member member) {
        return FindMemberRes.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .createDate(member.getCreateDate())
                .build();
    }
}

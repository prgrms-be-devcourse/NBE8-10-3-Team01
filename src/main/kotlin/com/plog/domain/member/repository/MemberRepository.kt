package com.plog.domain.member.repository

import com.plog.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

// TODO: Optional -> Member? 로 수정 필요 & MemberService 엘비스 연산자로 같이 수정
@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    fun findByNickname(nickname: String): Optional<Member>

    fun existsByEmail(email: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    fun findByEmail(email: String): Optional<Member>
}
package com.plog.domain.member.repository

import com.plog.domain.member.entity.Member
import com.plog.domain.member.entity.SocialAuth
import com.plog.domain.member.entity.SocialAuthProvider
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


/** TODO: 주석 채우기
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ExampleClass(String example)}  <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author minhee
 * @since 2026-02-25
 * @see
 */
@Repository
interface SocialAuthRepository : JpaRepository<SocialAuth, Long> {
    @EntityGraph(attributePaths = ["member"])
    fun findByProviderAndProviderId(provider: SocialAuthProvider, providerId: String): SocialAuth?

    fun existsByProviderAndProviderId(provider: SocialAuthProvider, providerId: String): Boolean

    // 연동 목록 조회 추가 -> 마이페이지에서 조회 하는 경우가 있다면 사용
    fun findAllByMember(member: Member): List<SocialAuth>
}
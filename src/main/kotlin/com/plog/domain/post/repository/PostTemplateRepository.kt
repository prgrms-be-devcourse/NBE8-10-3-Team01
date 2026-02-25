package com.plog.domain.post.repository

import com.plog.domain.post.entity.PostTemplate
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [PostTemplate] 엔티티에 대한 JPA 리포지토리입니다.
 */
interface PostTemplateRepository : JpaRepository<PostTemplate, Long> {
    /** 특정 회원이 소유한 템플릿 전체를 조회합니다. */
    fun findAllByMember_Id(memberId: Long): List<PostTemplate>
}

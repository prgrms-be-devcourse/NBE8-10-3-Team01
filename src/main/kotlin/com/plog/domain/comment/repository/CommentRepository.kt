package com.plog.domain.comment.repository

import com.plog.domain.comment.entity.Comment
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Spring Data JPA의 {@link JpaRepository}를 상속하여
 * 댓글(PostComment) 엔티티에 대한 기본적인 CRUD 기능을 제공합니다.
 *
 * 주요 기능:
 *   댓글 다건 조회 + 페이징 기능
 *   댓글 저장 및 삭제
 *   댓글 수정
 *
 * @author 노정원
 * @since 2026-02-23
 * @see Comment
 * @see JpaRepository
 */
interface CommentRepository : JpaRepository<Comment, Long> {

    @Query("""
        select c from Comment c 
        join fetch c.author m 
        left join fetch m.profileImage i 
        where c.post.id = :postId and c.parent is null 
        order by c.createDate asc
    """)
    fun findCommentsWithMemberAndImageByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): Slice<Comment>

    @Query("""
        select r from Comment r 
        join fetch r.author m 
        left join fetch m.profileImage i 
        where r.parent.id = :parentId 
        order by r.createDate asc
    """)
    fun findRepliesWithMemberAndImageByParentId(
        @Param("parentId") parentId: Long,
        pageable: Pageable
    ): Slice<Comment>

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId and c.parent is not null")
    fun deleteRepliesByPostId(@Param("postId") postId: Long)

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId and c.parent is null")
    fun deleteParentsByPostId(@Param("postId") postId: Long)

    fun existsByParent(parent: Comment): Boolean
}
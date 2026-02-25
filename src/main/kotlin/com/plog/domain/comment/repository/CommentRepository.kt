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
 *   데이터베이스 레벨의 원자적 업데이트(Atomic Update)를 통한 좋아요 카운트 관리
 *
 * 동시성 제어:
 *   좋아요 증감 시 {@link Modifying}과 직접적인 UPDATE 쿼리를 사용하여
 *   레이스 컨디션(Race Condition)을 방지하며, {@code clearAutomatically = true} 설정을 통해
 *   영속성 컨텍스트와의 데이터 일관성을 유지합니다.
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


    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    fun incrementLikeCount(commentId: Long): Int

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :commentId AND c.likeCount > 0")
    fun decrementLikeCount(commentId: Long): Int
}
package com.plog.domain.comment.entity

import com.plog.domain.member.entity.Member
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.apache.catalina.User

/**
 * 특정 댓글에 대한 사용자의 '좋아요' 이력을 관리하는 엔티티입니다.
 * <p>
 * 사용자와 댓글 간의 다대다(N:M) 관계를 일대다(1:N) - 다대일(N:1) 관계로 풀어낸 중간 엔티티이며,
 * 특정 사용자가 하나의 댓글에 중복으로 좋아요를 남기는 것을 방지합니다.
 *
 * <p><b>데이터 정합성:</b><br>
 * 데이터베이스 수준에서 {@code member_id}와 {@code comment_id}의 복합 유니크 제약조건을 설정하여
 * 애플리케이션의 레이스 컨디션 상황에서도 중복 데이터 생성을 원천 차단합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code CommentLike(member = member, comment = comment)}  <br>
 * 좋아요를 누른 회원과 대상 댓글을 인자로 받아 생성합니다. <br>
 *
 * <p><b>삭제 정책:</b><br>
 * '좋아요 취소' 시 해당 엔티티는 물리적으로 삭제(Hard Delete)됩니다.
 *
 * @author 노정원
 * @since 2026-02-25
 * @see Member
 * @see Comment
 */
@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "comment_id"])])
class CommentLike (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    val comment: Comment
)
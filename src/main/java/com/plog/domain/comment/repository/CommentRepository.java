package com.plog.domain.comment.repository;

import com.plog.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * <p>
 * Spring Data JPA의 {@link JpaRepository}를 상속하여
 * 댓글(PostComment) 엔티티에 대한 기본적인 CRUD 기능을 제공합니다.
 * </p>
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>댓글 다건 조회 + 페이징 기능</li>
 *   <li>댓글 저장 및 삭제</li>
 *   <li>댓글 수정</li>
 * </ul>
 *
 * @author 노정원
 * @since 2026-01-15
 * @see Comment
 * @see JpaRepository
 */
public interface CommentRepository extends JpaRepository<Comment, Long>{

    @EntityGraph(attributePaths = {"author"}) // N+1 방지
    Slice<Comment> findByPostIdAndParentIsNull(Long postId, Pageable pageable);

    // 대댓글 조회용 (추가)
    @EntityGraph(attributePaths = {"author"})
    Slice<Comment> findByParentId(Long parentId, Pageable pageable);

    boolean existsByParent(Comment parent);
}

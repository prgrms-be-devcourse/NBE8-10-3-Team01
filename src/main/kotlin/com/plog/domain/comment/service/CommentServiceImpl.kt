package com.plog.domain.comment.service

import com.plog.domain.comment.constant.CommentConstants
import com.plog.domain.comment.dto.CommentCreateReq
import com.plog.domain.comment.dto.CommentInfoRes
import com.plog.domain.comment.dto.ReplyInfoRes
import com.plog.domain.comment.entity.Comment
import com.plog.domain.comment.repository.CommentRepository
import com.plog.domain.member.repository.MemberRepository
import com.plog.domain.post.repository.PostRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.errorCode.CommentErrorCode
import com.plog.global.exception.errorCode.PostErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.exception.exceptions.CommentException
import com.plog.global.exception.exceptions.PostException
import org.springframework.data.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.PI

@Service
@Transactional(readOnly = true)
class CommentServiceImpl(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
): CommentService {

    @Transactional
    override fun createComment(postId: Long, memberId: Long, req: CommentCreateReq): Long{
        val member = memberRepository.findById(memberId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND) }

        val post = postRepository.findById(postId)
            .orElseThrow{
                PostException(PostErrorCode.POST_NOT_FOUND,
                    "[CommentServiceImpl#createComment] post not found",
                    "존재하지 않는 게시물입니다.")
            }

        val parentComment = req.parentCommentId?.let{ id ->
            commentRepository.findById(id).orElseThrow{
                CommentException(CommentErrorCode.COMMENT_NOT_FOUND,"부모 댓글 없음: $id", "부모 댓글이 존재하지 않습니다.")
            }.also{parentComment ->
                if(parentComment.post.id != postId){
                    throw CommentException(CommentErrorCode.INVALID_PARENT_COMMENT, "게시글 불일치", "해당 게시글의 댓글이 아닙니다.")
                }
            }
        }

        val comment = Comment(
            author = member,
            post = post,
            content = req.content,
            parent = parentComment,
            deleted = false
        )

        val savedComment = commentRepository.save(comment)

        return savedComment.id!!
    }

    @Transactional(readOnly = true)
    override fun getCommentsByPostId(postId: Long, pageNumber: Int): Slice<CommentInfoRes> {
        if(!postRepository.existsById(postId)){
            throw PostException(PostErrorCode.POST_NOT_FOUND,"게시물 없음: $postId", "존재하지 않는 게시물입니다.")
        }

        val pageable = PageRequest.of(
            pageNumber,
            CommentConstants.COMMENT_PAGE_SIZE,
            Sort.by(Sort.Direction.ASC, CommentConstants.DEFAULT_SORT_FIELD)
        )

        val comments = commentRepository.findCommentsWithMemberAndImageByPostId(postId, pageable)

        return comments.map { convertToCommentInfoRes(it) }
    }

    @Transactional(readOnly = true)
    override fun getRepliesByCommentId(commentId: Long, pageNumber: Int): Slice<ReplyInfoRes> {
        if(!commentRepository.existsById(commentId)){
            throw CommentException(CommentErrorCode.COMMENT_NOT_FOUND,
                "댓글 없음: $commentId",
                "존재하지 않는 댓글입니다.")
        }

        val pageable = PageRequest.of(
            pageNumber,
            CommentConstants.REPLY_PAGE_SIZE,
            Sort.by(Sort.Direction.ASC, CommentConstants.DEFAULT_SORT_FIELD)
        )

        return commentRepository.findRepliesWithMemberAndImageByParentId(commentId, pageable)
            .map(ReplyInfoRes::from)
    }

    @Transactional
    override fun updateComment(commentId: Long, memberId: Long, content: String) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow{
                CommentException(
                    CommentErrorCode.COMMENT_NOT_FOUND,
                    "[CommentService#updateComment] can't find comment with id: $commentId",
                    "존재하지 않는 댓글입니다."
                )
            }

        if(comment.author.id != memberId){
            throw AuthException(
                AuthErrorCode.USER_AUTH_FAIL,
                "[CommentService#updateComment] authorization failed. requestId: $commentId",
                "수정 권한이 없습니다."
            )
            }

        comment.modify(content)
    }

    @Transactional
    override fun deleteComment(commentId: Long, memberId: Long) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow{
                CommentException(
                    CommentErrorCode.COMMENT_NOT_FOUND,
                    "[CommentService#deleteComment] can't find comment with id: $commentId",
                    "존재하지 않는 댓글입니다.")
            }

        if(comment.author.id != memberId){
            throw AuthException(
                AuthErrorCode.USER_AUTH_FAIL,
                "[CommentService#deleteComment] authorization failed. requestId: $memberId",
                "삭제 권한이 없습니다."
            )
        }

        if(comment.deleted)
            return

        if(commentRepository.existsByParent(comment)){
            comment.softDelete()
        }else{
            commentRepository.delete(comment)
        }
    }

    private fun convertToCommentInfoRes(comment: Comment): CommentInfoRes {
        val replyPageable = PageRequest.of(0,
            CommentConstants.REPLY_PAGE_SIZE,
            Sort.by("createDate").ascending())

        val replySlice = commentRepository.findRepliesWithMemberAndImageByParentId(comment.id!!, replyPageable)

        val replyDtoSlice = replySlice.map{ ReplyInfoRes.from(it) }

        return CommentInfoRes.from(comment, replyDtoSlice)
    }
}
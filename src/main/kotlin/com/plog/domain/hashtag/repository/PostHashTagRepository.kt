package com.plog.domain.hashtag.repository

import com.plog.domain.hashtag.entity.PostHashTag
import org.springframework.data.jpa.repository.JpaRepository


interface PostHashTagRepository : JpaRepository<PostHashTag, Long> {
    fun existsByPostIdAndHashTagId(postId: Long, hashTagId: Long): Boolean

    fun deleteAllByPostId(postId: Long)
}

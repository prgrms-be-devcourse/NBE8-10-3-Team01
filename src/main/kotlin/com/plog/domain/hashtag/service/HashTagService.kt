package com.plog.domain.hashtag.service

import com.plog.domain.post.dto.PostListRes
import com.plog.domain.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface HashTagService {
    fun linkTags(post: Post, tagNames: List<String>?)
    fun updateTags(post: Post, newTagNames: List<String>?)
    fun searchPostsByTag(keyword: String?, pageable: Pageable): Page<PostListRes>
}
package com.plog.domain.hashtag.service

import com.plog.domain.hashtag.entity.HashTag
import com.plog.domain.hashtag.entity.PostHashTag
import com.plog.domain.hashtag.repository.HashTagRepository
import com.plog.domain.hashtag.repository.PostHashTagRepository
import com.plog.domain.post.dto.PostListRes
import com.plog.domain.post.entity.Post
import com.plog.domain.post.repository.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HashTagServiceImpl(
    private val hashTagRepository: HashTagRepository,
    private val postHashTagRepository: PostHashTagRepository,
    private val postRepository: PostRepository
) : HashTagService {


    @Transactional(readOnly = true)
    override fun searchPostsByTag(keyword: String?, pageable: Pageable): Page<PostListRes> {
        if (keyword.isNullOrBlank()) return Page.empty()
        val normalizedKeyword = keyword.trim().lowercase().replace(" ", "_")

        return postRepository.findByHashTagKeyword(normalizedKeyword, pageable)
            .map { post -> PostListRes.from(post) }
    }
}
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

    override fun linkTags(post: Post, tagNames: List<String>?) {
        if (tagNames.isNullOrEmpty()) return

        for (rawName in tagNames) {

            val normalizedName = rawName.trim().lowercase().replace(" ", "_")


            val hashTag = hashTagRepository.findByName(normalizedName)
                ?: hashTagRepository.save(HashTag(name = normalizedName))

            // 중복 연결 방지 로직
            if (!postHashTagRepository.existsByPostIdAndHashTagId(post.id!!, hashTag.id!!)) {
                val postHashTag = PostHashTag.builder()
                    .post(post)
                    .hashTag(hashTag)
                    .displayName(rawName)
                    .build()
                postHashTagRepository.save(postHashTag)
            }
        }
    }

    override fun updateTags(post: Post, newTagNames: List<String>?) {
        // 기존 연결된 태그 정보 삭제
        postHashTagRepository.deleteAllByPostId(post.id!!)
        // 새 태그 연결
        linkTags(post, newTagNames)
    }

    @Transactional(readOnly = true)
    override fun searchPostsByTag(keyword: String?, pageable: Pageable): Page<PostListRes> {
        if (keyword.isNullOrBlank()) return Page.empty()
        val normalizedKeyword = keyword.trim().lowercase().replace(" ", "_")
//
//        return postRepository.findByHashTagName(normalizedKeyword, pageable)
//            .map { PostListRes.from(it) }
    }
}
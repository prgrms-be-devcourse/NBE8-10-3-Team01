package com.plog.domain.hashtag.service


import com.plog.domain.post.dto.PostListRes
import com.plog.domain.post.repository.PostRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HashTagServiceImpl(
    private val postRepository: PostRepository
) : HashTagService {


    @Transactional(readOnly = true)
    override fun searchPostsByTag(hashTag: String?, pageable: Pageable): Slice<PostListRes> {
        if (hashTag.isNullOrBlank()) {
            return SliceImpl(emptyList(), pageable, false)
        }
        val normalizedKeyword = hashTag.trim().lowercase().replace(" ", "_")

        return postRepository.findByHashTagContaining(normalizedKeyword, pageable)
            .map { post -> PostListRes.from(post) }
    }
}
package com.plog.domain.hashtag.service


import com.plog.domain.post.dto.PostListRes
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface HashTagService {

    fun searchPostsByTag(hashTag: String?, pageable: Pageable): Slice<PostListRes>
}
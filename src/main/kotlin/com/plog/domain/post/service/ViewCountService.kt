package com.plog.domain.post.service

interface ViewCountService {
    fun incrementViewCount(postId: Long, userId: String)
    fun syncViewCountsToDb()
}

package com.plog.domain.hashtag.repository

import com.plog.domain.hashtag.entity.HashTag
import org.springframework.data.jpa.repository.JpaRepository


interface HashTagRepository :JpaRepository<HashTag, Long> {
    fun findByName(name: String): HashTag?
}
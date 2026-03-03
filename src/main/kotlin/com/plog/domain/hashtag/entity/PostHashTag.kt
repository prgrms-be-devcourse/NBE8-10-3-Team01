package com.plog.domain.hashtag.entity

import com.plog.domain.post.entity.Post
import com.plog.global.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "post_hashtag")
class PostHashTag (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    var hashTag: HashTag? = null,

    var displayName: String = ""

): BaseEntity()

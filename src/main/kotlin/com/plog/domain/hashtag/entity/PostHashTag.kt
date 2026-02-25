package com.plog.domain.hashtag.entity

import com.plog.domain.post.entity.Post
import com.plog.global.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "post_hashtag")
class PostHashTag (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    var hashTag: HashTag,

    var displayName: String

): BaseEntity() {

    companion object {
        fun builder() = Builder()


}
    class Builder {
        private var post: Post? = null
        private var hashTag: HashTag? = null
        private var displayName: String? = null

        fun post(post: Post) = apply { this.post = post }
        fun hashTag(hashTag: HashTag) = apply { this.hashTag = hashTag }
        fun displayName(displayName: String) = apply { this.displayName = displayName }

        fun build() = PostHashTag(
            post = post!!,
            hashTag = hashTag!!,
            displayName = displayName ?: ""
        )
    }
}
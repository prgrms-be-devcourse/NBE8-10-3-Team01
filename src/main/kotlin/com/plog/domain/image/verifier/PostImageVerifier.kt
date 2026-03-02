package com.plog.domain.image.verifier

import com.plog.domain.image.entity.Image
import com.plog.domain.post.repository.PostRepository
import org.springframework.stereotype.Component

@Component
class PostImageVerifier(
    private val postRepository: PostRepository
) : ImageUsageVerifier {

    override fun supports(domain: String) = domain == "POST"

    override fun isInUse(image: Image): Boolean {
        val domainId = image.domainId ?: return false
        val post = postRepository.findById(domainId).orElse(null) ?: return false
        return post.content.contains(image.accessUrl) ||
                post.thumbnail == image.accessUrl
    }
}

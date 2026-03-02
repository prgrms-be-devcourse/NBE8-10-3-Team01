package com.plog.domain.image.verifier

import com.plog.domain.image.entity.Image

interface ImageUsageVerifier {
    fun supports(domain: String): Boolean
    fun isInUse(image: Image): Boolean
}

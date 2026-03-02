package com.plog.domain.image.verifier

import com.plog.domain.image.entity.Image
import com.plog.domain.member.repository.MemberRepository
import org.springframework.stereotype.Component

@Component
class ProfileImageVerifier(
    private val memberRepository: MemberRepository
) : ImageUsageVerifier {

    override fun supports(domain: String) = domain == "PROFILE"

    override fun isInUse(image: Image): Boolean {
        val domainId = image.domainId ?: return false
        val member = memberRepository.findById(domainId).orElse(null) ?: return false
        return member.profileImage?.accessUrl == image.accessUrl
    }
}

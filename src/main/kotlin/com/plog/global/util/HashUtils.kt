package com.plog.global.util

import java.security.MessageDigest
import java.util.HexFormat

/**
 * 보안 및 개인정보 보호를 위한 해싱 유틸리티 클래스입니다.
 */
object HashUtils {

    /**
     * 입력 문자열을 SHA-256 알고리즘을 사용하여 해싱합니다.
     *
     * @param input 해싱할 원본 문자열 (IP 주소, 회원 ID 등)
     * @return 64자리의 16진수 해시 문자열
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(hashBytes)
    }
}

package com.plog.global.util

import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class TimeUtil {  // ← 대문자 T로 변경!

    fun getNow(): LocalDateTime = LocalDateTime.now()

    fun getNowKST(): LocalDateTime = LocalDateTime.now(kstZoneId)

    companion object {
        private val kstZoneId = ZoneId.of("Asia/Seoul")
    }
}
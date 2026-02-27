package com.plog.domain.post.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ViewCountScheduler(private val viewCountSyncService: ViewCountSyncService) {

    /**
     * Every 10 minutes, sync Redis view counts to DB.
     */
    @Scheduled(cron = "0 0/10 * * * *")
    fun syncViewCounts() {
        viewCountSyncService.syncViewCountsToDb()
    }
}

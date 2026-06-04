package com.noom.interview.fullstack.sleep

import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID


@RestController
class SleepLogController(private val sleepLogService: SleepLogService) {

    @PostMapping("/user/{userId}/sleep/add")
    fun addSleepLog(@RequestBody request: SleepLog) : ResponseEntity<Map<String, String>> {
        val sleepId = sleepLogService.addSleepLog(request)
        return ResponseEntity(mapOf("id" to sleepId.toString()), HttpStatus.OK)
    }

    @GetMapping("/user/{userId}/sleep/last")
    fun getSleepLog(@PathVariable userId: UUID) : SleepLog? {
        val sleepLog: SleepLog? = sleepLogService.getLastNightSleep(userId)
        return sleepLog
    }

    @GetMapping("/user/{userId}/sleep/average")
    fun getSleepLogAverage(
        @PathVariable userId: UUID,
        @RequestParam("lookback_days", defaultValue = "30") lookbackDays: Int,
    ) : Map<String?, String?> {
        return sleepLogService.getAverageStats(userId, lookbackDays)
    }
}

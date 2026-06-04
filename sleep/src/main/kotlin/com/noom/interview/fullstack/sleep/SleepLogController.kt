package com.noom.interview.fullstack.sleep

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid


@RestController
class SleepLogController(private val sleepLogService: SleepLogService) {

    @PostMapping("/user/{userId}/sleep/add")
    fun addSleepLog(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: SleepLog,
    ): ResponseEntity<Map<String, String>> {
        if (userId != request.userId) {
            throw IllegalArgumentException("Path variable userId does not match request body userId")
        }
        val sleepId = sleepLogService.addSleepLog(request)
        return ResponseEntity(mapOf("id" to sleepId.toString()), HttpStatus.OK)
    }

    @GetMapping("/user/{userId}/sleep/last")
    fun getSleepLog(@PathVariable userId: UUID): ResponseEntity<*> {
        val sleepLog: SleepLog? = sleepLogService.getLastNightSleep(userId)
        return if (sleepLog != null) {
            ResponseEntity.ok(sleepLog)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("not_found", "No sleep log found for user"))
        }
    }

    @GetMapping("/user/{userId}/sleep/average")
    fun getSleepLogAverage(
        @PathVariable userId: UUID,
        @RequestParam("lookback_days", defaultValue = "30") lookbackDays: Int,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(sleepLogService.getAverageStats(userId, lookbackDays))
    }
}

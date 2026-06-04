package com.noom.interview.fullstack.sleep

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.validation.constraints.NotNull


data class SleepLog(
    val id: UUID? = null,
    @field:NotNull(message = "userId is required")
    val userId: UUID,
    @field:NotNull(message = "sleepDate is required")
    val sleepDate: LocalDate,
    @field:NotNull(message = "startTime is required")
    val startTime: LocalTime,
    @field:NotNull(message = "endTime is required")
    val endTime: LocalTime,
    @field:NotNull(message = "duration is required")
    val duration: Duration,
    @field:NotNull(message = "quality is required")
    val quality: SleepQuality,
)

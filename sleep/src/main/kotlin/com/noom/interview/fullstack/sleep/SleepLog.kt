package com.noom.interview.fullstack.sleep

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID


data class SleepLog(
    val id: UUID? = null,
    val userId: UUID,
    val sleepDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val duration: Duration,
    val quality: SleepQuality,
)

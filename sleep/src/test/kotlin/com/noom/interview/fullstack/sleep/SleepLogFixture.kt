package com.noom.interview.fullstack.sleep

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID


@Suppress("LongParameterList")
fun newSleepLog(
    userId: UUID,
    sleepDate: LocalDate = LocalDate.now(),
    startTime: LocalTime = LocalTime.of(23, 0),
    endTime: LocalTime = LocalTime.of(7, 0),
    duration: Duration = Duration.between(startTime, endTime).plusHours(24),
    quality: SleepQuality = SleepQuality.GOOD,
) = SleepLog(
    userId = userId,
    sleepDate = sleepDate,
    startTime = startTime,
    endTime = endTime,
    duration = duration,
    quality = quality,
)

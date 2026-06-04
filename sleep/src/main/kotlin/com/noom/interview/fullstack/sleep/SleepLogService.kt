package com.noom.interview.fullstack.sleep

import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID


@Service
class SleepLogService(val sleepLogDao: SleepLogDao) {

    fun addSleepLog(sleepLog: SleepLog): UUID = sleepLogDao.insert(sleepLog)

    fun getLastNightSleep(userId: UUID): SleepLog? {
        val yesterday = LocalDate.now().minusDays(1)
        return sleepLogDao.findByUserIdAndDate(userId, yesterday)
    }

    @Suppress("MagicNumber", "ReturnCount")
    fun getAverageStats(userId: UUID, lookBackDays: Int = 30): Map<String?, String?> {
        val to = LocalDate.now().minusDays(1)
        val from = to.minusDays(lookBackDays.toLong())

        if (to.isBefore(from) == true) {
            throw IllegalArgumentException("lookback_days must be positive")
        }

        val sleepLogList = sleepLogDao.findByUserIdAndDateRange(userId, from, to)

        if (sleepLogList.isEmpty()) return mapOf()

        val secondsInDay = 86_400
        val secondsUntilNoon = 43_200

        var goodDays = 0
        var badDays = 0
        var okDays = 0

        var avgDuration = Duration.ZERO
        var totalStartTimeSeconds = 0
        var totalEndTimeSeconds = 0

        for (sleepLog in sleepLogList) {
            when (sleepLog.quality) {
                SleepQuality.GOOD -> goodDays++
                SleepQuality.BAD -> badDays++
                SleepQuality.OK -> okDays++
            }
            // Just add durations inside the loop. We divide by number of days after the loop exits
            avgDuration = avgDuration.plus(sleepLog.duration)

            // End times are assumed to be after midnight
            totalEndTimeSeconds += sleepLog.endTime.toSecondOfDay()

            // Start times can straddle mignight. So we add an extra day worth of seconds if it's beyond midnight.
            // After averaging the start time (in seconds) we modulo-divide by seconds-in-a-day to get the time.
            // Toy example:
            // Average of 11:59pm and 00:01am should be 00:00am.
            // 11:59pm -> 86340 seconds. 00:01am -> 60 seconds. Adding and averaging gives us 86400/2 = 43200 -> 12pm.
            // Using our logic:
            // 11:59am -> 86340 seconds. 00:01am -> 60 seconds + 86400 (1 day) = 86460 seconds.
            // (86460 + 86340)/2 = 172800/2 = 86400 seconds -> 00:00am. Which is correct.
            var currentStartTimeSeconds = sleepLog.startTime.toSecondOfDay()
            if (currentStartTimeSeconds < secondsUntilNoon) {
                currentStartTimeSeconds += secondsInDay
            }
            totalStartTimeSeconds += currentStartTimeSeconds
        }

        avgDuration = Duration.ofSeconds((avgDuration.getSeconds() / sleepLogList.size).toLong())
        val avgStartTime = LocalTime.ofSecondOfDay(
            ((totalStartTimeSeconds / sleepLogList.size) % secondsInDay)
                .toLong()
            )
        val avgEndTime = LocalTime.ofSecondOfDay((totalEndTimeSeconds / sleepLogList.size).toLong())

        return mapOf(
            "average_duration" to avgDuration.toString(),
            "good_days" to goodDays.toString(),
            "bad_days" to badDays.toString(),
            "ok_days" to okDays.toString(),
            "average_start" to avgStartTime.toString(),
            "average_end" to avgEndTime.toString(),
            "range_start" to from.toString(),
            "range_end" to to.toString(),
        )
    }
}

package com.noom.interview.fullstack.sleep

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SleepLogServiceTest {

    @Mock
    private lateinit var sleepLogDao: SleepLogDao

    @InjectMocks
    private lateinit var sleepLogService: SleepLogService

    private val userId = UUID.randomUUID()
    private val yesterday = LocalDate.now().minusDays(1)

    @Nested
    inner class AddSleepLog {

        @Test
        fun `should delegate to DAO and return UUID`() {
            val sleepLog = newSleepLog(userId)
            val expectedUuid = UUID.randomUUID()
            Mockito.`when`(sleepLogDao.findByUserIdAndDate(sleepLog.userId, sleepLog.sleepDate)).thenReturn(null)
            Mockito.`when`(sleepLogDao.insert(sleepLog)).thenReturn(expectedUuid)

            val result = sleepLogService.addSleepLog(sleepLog)

            assertThat(result).isEqualTo(expectedUuid)
            Mockito.verify(sleepLogDao).findByUserIdAndDate(sleepLog.userId, sleepLog.sleepDate)
            Mockito.verify(sleepLogDao).insert(sleepLog)
        }

        @Test
        fun `should throw DuplicateSleepLogException when sleep log already exists for that date`() {
            val sleepLog = newSleepLog(userId)
            val existing = newSleepLog(userId, sleepDate = sleepLog.sleepDate)
            Mockito.`when`(sleepLogDao.findByUserIdAndDate(sleepLog.userId, sleepLog.sleepDate)).thenReturn(existing)

            assertThatThrownBy { sleepLogService.addSleepLog(sleepLog) }
                .isInstanceOf(DuplicateSleepLogException::class.java)
                .hasMessage("A sleep log already exists for this user on ${sleepLog.sleepDate}")

            Mockito.verify(sleepLogDao).findByUserIdAndDate(sleepLog.userId, sleepLog.sleepDate)
            Mockito.verify(sleepLogDao, Mockito.never()).insert(sleepLog)
        }
    }

    @Nested
    inner class GetLastNightSleep {

        @Test
        fun `should return yesterday sleep log`() {
            val sleepLog = newSleepLog(userId, sleepDate = yesterday)
            Mockito.`when`(sleepLogDao.findByUserIdAndDate(userId, yesterday)).thenReturn(sleepLog)

            val result = sleepLogService.getLastNightSleep(userId)

            assertThat(result).isEqualTo(sleepLog)
        }

        @Test
        fun `should return null when no log exists for yesterday`() {
            Mockito.`when`(sleepLogDao.findByUserIdAndDate(userId, yesterday)).thenReturn(null)

            val result = sleepLogService.getLastNightSleep(userId)

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class GetAverageStats {

        @Test
        fun `should return map with correct averages for multiple records`() {
            val lookBackDays = 30
            val to = yesterday
            val from = to.minusDays(lookBackDays.toLong())
            val sleepLogs = listOf(
                newSleepLog(userId,
                    sleepDate = from,
                    startTime = LocalTime.of(23, 0),
                    endTime = LocalTime.of(7, 0),
                    duration = Duration.ofHours(8),
                    quality = SleepQuality.GOOD,
                ),
                newSleepLog(userId,
                    sleepDate = to,
                    startTime = LocalTime.of(22, 30),
                    endTime = LocalTime.of(6, 30),
                    duration = Duration.ofHours(8),
                    quality = SleepQuality.BAD,
                ),
                newSleepLog(userId,
                    sleepDate = from.plusDays(1),
                    startTime = LocalTime.of(23, 30),
                    endTime = LocalTime.of(8, 0),
                    duration = Duration.ofHours(8).plusMinutes(30),
                    quality = SleepQuality.OK,
                ),
            )
            Mockito.`when`(sleepLogDao.findByUserIdAndDateRange(userId, from, to)).thenReturn(sleepLogs)

            val result = sleepLogService.getAverageStats(userId, lookBackDays)

            assertThat(result).containsEntry("good_days", "1")
            assertThat(result).containsEntry("bad_days", "1")
            assertThat(result).containsEntry("ok_days", "1")
            assertThat(result).containsEntry("average_duration", "PT8H10M")
            assertThat(result).containsEntry("average_end", "07:10")
            assertThat(result).containsEntry("average_start", "23:00")
            assertThat(result).containsEntry("range_start", from.toString())
            assertThat(result).containsEntry("range_end", to.toString())
        }

        @Test
        fun `should handle midnight straddle start times`() {
            val lookBackDays = 30
            val to = yesterday
            val from = to.minusDays(lookBackDays.toLong())
            val sleepLogs = listOf(
                newSleepLog(userId,
                    sleepDate = from,
                    startTime = LocalTime.of(23, 59),
                    endTime = LocalTime.of(7, 0),
                    duration = Duration.ofHours(7).plusMinutes(1),
                    quality = SleepQuality.GOOD,
                ),
                newSleepLog(userId,
                    sleepDate = to,
                    startTime = LocalTime.of(0, 1),
                    endTime = LocalTime.of(7, 30),
                    duration = Duration.ofHours(7).plusMinutes(29),
                    quality = SleepQuality.GOOD,
                ),
            )
            Mockito.`when`(sleepLogDao.findByUserIdAndDateRange(userId, from, to)).thenReturn(sleepLogs)

            val result = sleepLogService.getAverageStats(userId, lookBackDays)

            assertThat(result).containsEntry("average_start", "00:00")
            assertThat(result).containsEntry("average_end", "07:15")
            assertThat(result).containsEntry("average_duration", "PT7H15M")
        }

        @Test
        fun `should return empty map when no records exist`() {
            val lookBackDays = 30
            val to = yesterday
            val from = to.minusDays(lookBackDays.toLong())
            Mockito.`when`(sleepLogDao.findByUserIdAndDateRange(userId, from, to)).thenReturn(emptyList())

            val result = sleepLogService.getAverageStats(userId, lookBackDays)

            assertThat(result).isEmpty()
        }

        @Test
        fun `should throw exception when lookBackDays is negative`() {
            assertThatThrownBy { sleepLogService.getAverageStats(userId, lookBackDays = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("lookback_days must be positive")
        }

        @Test
        fun `should handle single record`() {
            val lookBackDays = 30
            val to = yesterday
            val from = to.minusDays(lookBackDays.toLong())
            val sleepLog = newSleepLog(userId,
                sleepDate = from,
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(6, 0),
                duration = Duration.ofHours(8),
                quality = SleepQuality.GOOD,
            )
            Mockito.`when`(sleepLogDao.findByUserIdAndDateRange(userId, from, to)).thenReturn(listOf(sleepLog))

            val result = sleepLogService.getAverageStats(userId, lookBackDays)

            assertThat(result).containsEntry("good_days", "1")
            assertThat(result).containsEntry("average_start", "22:00")
            assertThat(result).containsEntry("average_end", "06:00")
            assertThat(result).containsEntry("average_duration", "PT8H")
        }

        @Test
        fun `should handle all same quality`() {
            val lookBackDays = 30
            val to = yesterday
            val from = to.minusDays(lookBackDays.toLong())
            val sleepLogs = listOf(
                newSleepLog(userId, sleepDate = from, quality = SleepQuality.GOOD),
                newSleepLog(userId, sleepDate = from.plusDays(1), quality = SleepQuality.GOOD),
                newSleepLog(userId, sleepDate = to, quality = SleepQuality.GOOD),
            )
            Mockito.`when`(sleepLogDao.findByUserIdAndDateRange(userId, from, to)).thenReturn(sleepLogs)

            val result = sleepLogService.getAverageStats(userId, lookBackDays)

            assertThat(result).containsEntry("good_days", "3")
            assertThat(result).containsEntry("bad_days", "0")
            assertThat(result).containsEntry("ok_days", "0")
        }

        @Test
        fun `should use default lookBackDays of 30`() {
            val to = yesterday
            val from = to.minusDays(30)
            Mockito.`when`(sleepLogDao.findByUserIdAndDateRange(userId, from, to)).thenReturn(emptyList())

            val result = sleepLogService.getAverageStats(userId)

            assertThat(result).isEmpty()
            Mockito.verify(sleepLogDao).findByUserIdAndDateRange(userId, from, to)
        }
    }
}

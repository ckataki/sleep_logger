package com.noom.interview.fullstack.sleep

import com.noom.interview.fullstack.sleep.SleepApplication.Companion.UNIT_TEST_PROFILE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles(UNIT_TEST_PROFILE)
@Transactional
class SleepLogDaoTest {

    @Autowired
    private lateinit var sleepLogDao: SleepLogDao

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val userId = UUID.randomUUID()
    private val userId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM sleep_log", mapOf<String, Any>())
        jdbc.update("DELETE FROM users", mapOf<String, Any>())
        jdbc.update(
            "INSERT INTO users (id, username) VALUES (:id, :username)",
            mapOf("id" to userId, "username" to "test-user-1"),
        )
        jdbc.update(
            "INSERT INTO users (id, username) VALUES (:id, :username)",
            mapOf("id" to userId2, "username" to "test-user-2"),
        )
    }

    @Test
    fun `insert should return a non-null UUID`() {
        val id = sleepLogDao.insert(newSleepLog(userId))

        assertThat(id).isNotNull
    }

    @Test
    fun `insert should persist all fields correctly`() {
        val sleepLog = newSleepLog(userId,
            sleepDate = LocalDate.of(2024, 1, 15),
            startTime = LocalTime.of(23, 0),
            endTime = LocalTime.of(7, 0),
            duration = Duration.ofHours(8),
            quality = SleepQuality.GOOD,
        )

        val id = sleepLogDao.insert(sleepLog)
        val retrieved = sleepLogDao.findById(id)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.id).isEqualTo(id)
        assertThat(retrieved.userId).isEqualTo(userId)
        assertThat(retrieved.sleepDate).isEqualTo(LocalDate.of(2024, 1, 15))
        assertThat(retrieved.startTime).isEqualTo(LocalTime.of(23, 0))
        assertThat(retrieved.endTime).isEqualTo(LocalTime.of(7, 0))
        assertThat(retrieved.duration).isEqualTo(Duration.ofHours(8))
        assertThat(retrieved.quality).isEqualTo(SleepQuality.GOOD)
    }

    @Test
    fun `findById should return null for non-existent id`() {
        val result = sleepLogDao.findById(UUID.randomUUID())

        assertThat(result).isNull()
    }

    @Test
    fun `findByUserId should return all records for a user ordered by sleep_date descending`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)

        sleepLogDao.insert(newSleepLog(userId, sleepDate = twoDaysAgo, quality = SleepQuality.BAD))
        sleepLogDao.insert(newSleepLog(userId, sleepDate = yesterday, quality = SleepQuality.OK))
        sleepLogDao.insert(newSleepLog(userId = userId2, sleepDate = today, quality = SleepQuality.GOOD))

        val result = sleepLogDao.findByUserId(userId)

        assertThat(result).hasSize(2)
        assertThat(result[0].sleepDate).isEqualTo(yesterday)
        assertThat(result[1].sleepDate).isEqualTo(twoDaysAgo)
    }

    @Test
    fun `findByUserId should return empty list for user with no records`() {
        val result = sleepLogDao.findByUserId(UUID.randomUUID())

        assertThat(result).isEmpty()
    }

    @Test
    fun `findByUserIdAndDate should return the correct record`() {
        val date = LocalDate.of(2024, 6, 1)
        sleepLogDao.insert(newSleepLog(userId, sleepDate = date))

        val result = sleepLogDao.findByUserIdAndDate(userId, date)

        assertThat(result).isNotNull
        assertThat(result!!.sleepDate).isEqualTo(date)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `findByUserIdAndDate should return null for a date with no record`() {
        val result = sleepLogDao.findByUserIdAndDate(userId, LocalDate.of(2024, 6, 1))

        assertThat(result).isNull()
    }

    @Test
    fun `findByUserIdAndDateRange should return records within the date range`() {
        val from = LocalDate.of(2024, 6, 1)
        val to = LocalDate.of(2024, 6, 10)

        sleepLogDao.insert(newSleepLog(userId, sleepDate = from))
        sleepLogDao.insert(newSleepLog(userId, sleepDate = LocalDate.of(2024, 6, 5)))
        sleepLogDao.insert(newSleepLog(userId, sleepDate = to))
        sleepLogDao.insert(newSleepLog(userId, sleepDate = LocalDate.of(2024, 6, 15)))

        val result = sleepLogDao.findByUserIdAndDateRange(userId, from, to)

        assertThat(result).hasSize(3)
    }

    @Test
    fun `findByUserIdAndDateRange should return empty list when no records exist in range`() {
        sleepLogDao.insert(newSleepLog(userId, sleepDate = LocalDate.of(2024, 1, 1)))

        val result = sleepLogDao.findByUserIdAndDateRange(
            userId, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 10)
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `records for different users should be isolated`() {
        sleepLogDao.insert(newSleepLog(userId = userId, sleepDate = LocalDate.of(2024, 6, 1)))
        sleepLogDao.insert(newSleepLog(userId = userId2, sleepDate = LocalDate.of(2024, 6, 1)))

        val result1 = sleepLogDao.findByUserId(userId)
        val result2 = sleepLogDao.findByUserId(userId2)

        assertThat(result1).hasSize(1)
        assertThat(result2).hasSize(1)
    }
}

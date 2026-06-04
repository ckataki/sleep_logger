package com.noom.interview.fullstack.sleep

import com.noom.interview.fullstack.sleep.SleepApplication.Companion.UNIT_TEST_PROFILE
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.Mockito
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(UNIT_TEST_PROFILE)
class SleepLogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var sleepLogService: SleepLogService

    private val userId = UUID.randomUUID()

    @Test
    fun `addSleepLog should return 200 with id`() {
        val id = UUID.randomUUID()
        val sleepLog = SleepLog(
            userId = userId,
            sleepDate = LocalDate.of(2024, 1, 15),
            startTime = LocalTime.of(23, 0),
            endTime = LocalTime.of(7, 0),
            duration = Duration.ofHours(8),
            quality = SleepQuality.GOOD,
        )
        Mockito.`when`(sleepLogService.addSleepLog(sleepLog)).thenReturn(id)

        mockMvc.perform(post("/user/$userId/sleep/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                // language=json
                """
                {
                    "user_id": "$userId",
                    "sleep_date": "2024-01-15",
                    "start_time": "23:00",
                    "end_time": "07:00",
                    "duration": "PT8H",
                    "quality": "GOOD"
                }
                """.trimIndent(),
            ))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `addSleepLog should return 400 when request body is empty`() {
        mockMvc.perform(post("/user/$userId/sleep/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLastNightSleep should return sleep log when found`() {
        val id = UUID.randomUUID()
        val sleepLog = SleepLog(
            id = id,
            userId = userId,
            sleepDate = LocalDate.of(2024, 1, 15),
            startTime = LocalTime.of(23, 0),
            endTime = LocalTime.of(7, 0),
            duration = Duration.ofHours(8),
            quality = SleepQuality.GOOD,
        )
        Mockito.`when`(sleepLogService.getLastNightSleep(userId)).thenReturn(sleepLog)

        mockMvc.perform(get("/user/$userId/sleep/last"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.user_id").value(userId.toString()))
            .andExpect(jsonPath("$.sleep_date").value("2024-01-15"))
            .andExpect(jsonPath("$.start_time").value("23:00:00"))
            .andExpect(jsonPath("$.end_time").value("07:00:00"))
            .andExpect(jsonPath("$.duration").value("PT8H"))
            .andExpect(jsonPath("$.quality").value("GOOD"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `getLastNightSleep should return 404 when no log exists`() {
        Mockito.`when`(sleepLogService.getLastNightSleep(userId)).thenReturn(null)

        mockMvc.perform(get("/user/$userId/sleep/last"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("not_found"))
            .andExpect(jsonPath("$.message").value("No sleep log found for user"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `getSleepLogAverage should return stats map`() {
        val stats: Map<String?, String?> = mapOf(
            "average_duration" to "PT8H",
            "good_days" to "2",
            "bad_days" to "1",
            "ok_days" to "0",
            "average_start" to "23:00",
            "average_end" to "07:00",
            "range_start" to "2024-01-01",
            "range_end" to "2024-01-31",
        )
        Mockito.`when`(sleepLogService.getAverageStats(userId, 30)).thenReturn(stats)

        mockMvc.perform(get("/user/$userId/sleep/average")
            .param("lookback_days", "30"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.average_duration").value("PT8H"))
            .andExpect(jsonPath("$.good_days").value("2"))
            .andExpect(jsonPath("$.bad_days").value("1"))
            .andExpect(jsonPath("$.ok_days").value("0"))
            .andExpect(jsonPath("$.average_start").value("23:00"))
            .andExpect(jsonPath("$.average_end").value("07:00"))
            .andExpect(jsonPath("$.range_start").value("2024-01-01"))
            .andExpect(jsonPath("$.range_end").value("2024-01-31"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `addSleepLog should return 400 when path userId does not match body userId`() {
        val otherUserId = UUID.randomUUID()

        mockMvc.perform(post("/user/$userId/sleep/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                // language=json
                """
                {
                    "user_id": "$otherUserId",
                    "sleep_date": "2024-01-15",
                    "start_time": "23:00",
                    "end_time": "07:00",
                    "duration": "PT8H",
                    "quality": "GOOD"
                }
                """.trimIndent(),
            ))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message").value("Path variable userId does not match request body userId"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `getSleepLogAverage should return 400 when lookback_days is negative`() {
        Mockito.`when`(sleepLogService.getAverageStats(userId, -1))
            .thenThrow(IllegalArgumentException("lookback_days must be positive"))

        mockMvc.perform(get("/user/$userId/sleep/average")
            .param("lookback_days", "-1"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message").value("lookback_days must be positive"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        Mockito.verify(sleepLogService).getAverageStats(userId, -1)
    }

    @Test
    fun `getSleepLogAverage should use default lookback_days when param omitted`() {
        val stats: Map<String?, String?> = mapOf(
            "average_duration" to "PT8H",
            "good_days" to "1",
            "bad_days" to "0",
            "ok_days" to "0",
            "average_start" to "23:00",
            "average_end" to "07:00",
            "range_start" to "2024-01-01",
            "range_end" to "2024-01-31",
        )
        Mockito.`when`(sleepLogService.getAverageStats(userId, 30)).thenReturn(stats)

        mockMvc.perform(get("/user/$userId/sleep/average"))
            .andExpect(status().isOk)

        Mockito.verify(sleepLogService).getAverageStats(userId, 30)
    }

    @Test
    fun `getSleepLogAverage should use custom lookback_days when provided`() {
        val stats: Map<String?, String?> = mapOf(
            "average_duration" to "PT8H",
            "good_days" to "1",
            "bad_days" to "0",
            "ok_days" to "0",
            "average_start" to "23:00",
            "average_end" to "07:00",
            "range_start" to "2024-01-07",
            "range_end" to "2024-01-07",
        )
        Mockito.`when`(sleepLogService.getAverageStats(userId, 7)).thenReturn(stats)

        mockMvc.perform(get("/user/$userId/sleep/average")
            .param("lookback_days", "7"))
            .andExpect(status().isOk)

        Mockito.verify(sleepLogService).getAverageStats(userId, 7)
    }
}

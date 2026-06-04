package com.noom.interview.fullstack.sleep

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

import java.sql.ResultSet
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID


@Repository
class SleepLogDao(private val jdbc: NamedParameterJdbcTemplate) {

    companion object {
        private fun mapRow(resultSet: ResultSet) =  SleepLog(
            id = UUID.fromString(resultSet.getString("id")),
            userId = UUID.fromString(resultSet.getString("user_id")),
            sleepDate = resultSet.getDate("sleep_date").toLocalDate(),
            startTime = resultSet.getTime("start_time").toLocalTime(),
            endTime = resultSet.getTime("end_time").toLocalTime(),
            duration = Duration.ofSeconds(resultSet.getInt("duration").toLong()),
            quality = SleepQuality.valueOf(resultSet.getString("quality")),
        )
    }

    fun insert(log: SleepLog): UUID {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update(
            """
                INSERT INTO sleep_log (user_id, sleep_date, start_time, end_time, duration, quality)
                VALUES (:userId, :sleepDate, :startTime, :endTime, :duration, :quality::varchar)
            """
                .trimIndent(),
            MapSqlParameterSource()
                .addValue("userId", log.userId)
                .addValue("sleepDate", log.sleepDate)
                .addValue("startTime", log.startTime)
                .addValue("endTime", log.endTime)
                .addValue("duration", log.duration.getSeconds().toInt())
                .addValue("quality", log.quality.name),
            keyHolder,
            arrayOf("id"),
        )
        return requireNotNull(keyHolder.keys?.get("id") as? UUID) { "Failed to retrieve generated id" }
    }

    fun findById(id: UUID): SleepLog? =
            jdbc.query(
                """
                    SELECT id, user_id, sleep_date, start_time, end_time, duration, quality FROM sleep_log
                    WHERE id = :id
                """
                    .trimIndent(),
                MapSqlParameterSource("id", id)
            ) { result, _ -> mapRow(result) }.firstOrNull()

    fun findByUserId(userId: UUID): List<SleepLog> =
        jdbc.query(
            """
                SELECT id, user_id, sleep_date, start_time, end_time, duration, quality FROM sleep_log
                WHERE user_id = :userId
                ORDER BY sleep_date DESC
            """
                .trimIndent(),
            MapSqlParameterSource("userId", userId)
        ) { result, _ -> mapRow(result) }

    fun findByUserIdAndDate(userId: UUID, sleepDate: LocalDate): SleepLog? =
        jdbc.query(
            """
                SELECT id, user_id, sleep_date, start_time, end_time, duration, quality FROM sleep_log
                WHERE user_id = :userId
                  AND sleep_date = :sleepDate
            """
                .trimIndent(),
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sleepDate", sleepDate)
            ) { result, _ -> mapRow(result) }.firstOrNull()


    fun findByUserIdAndDateRange(userId: UUID, from: LocalDate, to: LocalDate): List<SleepLog> =
        jdbc.query(
            """
                SELECT id, user_id, sleep_date, start_time, end_time, duration, quality FROM sleep_log
                WHERE user_id = :userId
                  AND sleep_date BETWEEN :from AND :to
                ORDER BY sleep_date DESC
            """
                .trimIndent(),
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", from)
                .addValue("to", to)
        ) { result, _ -> mapRow(result) }
}

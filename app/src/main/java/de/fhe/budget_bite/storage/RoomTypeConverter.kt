package de.fhe.budget_bite.storage

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object RoomTypeConverter {
    @TypeConverter
    fun fromTimeStamp(timestamp: Long?): LocalDateTime? {
        return timestamp?.let {
            val instant = Instant.ofEpochMilli(timestamp)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        }
    }

    @TypeConverter
    fun localDateTimeToTimestamp(localDateTime: LocalDateTime?): Long? {
        return localDateTime?.let {
            val zoneId = ZoneId.systemDefault()
            val zonedDateTime = localDateTime.atZone(zoneId)
            zonedDateTime.toEpochSecond() * 1000
        }
    }
}
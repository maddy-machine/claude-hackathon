package com.runanywhere.startup_hackathon20

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String {
        return value.name
    }

    @TypeConverter
    fun toEventType(value: String): EventType {
        return EventType.valueOf(value)
    }
}

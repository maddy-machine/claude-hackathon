package com.runanywhere.startup_hackathon20

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.util.UUID

class EventViewModel : ViewModel() {
    val events = mutableStateListOf<Event>()

    fun addEvent(event: Event) {
        events.add(event.copy(id = UUID.randomUUID().toString()))
    }

    fun getTasksForEvent(eventId: String): List<Task> {
        return emptyList() // Placeholder
    }

    fun toggleTask(eventId: String, taskId: String) {
        // Placeholder
    }
}

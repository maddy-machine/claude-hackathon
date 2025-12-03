package com.runanywhere.startup_hackathon20

import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun insert(event: Event) {
        eventDao.insert(event)
    }
}

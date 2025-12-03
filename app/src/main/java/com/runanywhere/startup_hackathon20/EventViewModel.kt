package com.runanywhere.startup_hackathon20

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventRepository
    val events: LiveData<List<Event>>

    init {
        val eventDao = EventDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)
        events = repository.allEvents.asLiveData()
    }

    fun addEvent(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(event)
    }

    fun getTasksForEvent(eventId: Long): List<Task> {
        // This should be fetched from the database
        return emptyList()
    }

    fun toggleTask(eventId: Long, taskId: String) {
        // This should update the database
    }

    fun getGuestsForEvent(eventId: Long): List<Guest> {
        // This should be fetched from the database
        return emptyList()
    }

    fun addGuest(eventId: Long, guest: Guest) {
        // This should be saved to the database
    }

    fun updateGuestRsvp(eventId: Long, guestId: String, newStatus: RSVPStatus) {
        // This should update the database
    }

    fun getExpensesForEvent(eventId: Long): List<Expense> {
        // This should be fetched from the database
        return emptyList()
    }

    fun addExpense(eventId: Long, expense: Expense) {
        // This should be saved to the database
    }

    fun scheduleReminder(context: Context, event: Event) {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putString("EVENT_NAME", event.name)
            .build()

        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(10, TimeUnit.SECONDS) // For testing, trigger after 10 seconds
            .build()

        workManager.enqueue(notificationWorkRequest)
    }
}

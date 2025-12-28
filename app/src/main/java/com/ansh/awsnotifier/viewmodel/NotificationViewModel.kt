package com.ansh.awsnotifier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.ansh.awsnotifier.data.NotificationRepository

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val allNotifications = repository.getAll().asLiveData()

    fun search(query: String) = repository.search(query).asLiveData()
}

package com.ansh.awsnotifier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.ansh.awsnotifier.data.NotificationDao

class NotificationViewModel(
    private val dao: NotificationDao
) : ViewModel() {

    val allNotifications = dao.getAll().asLiveData()
}

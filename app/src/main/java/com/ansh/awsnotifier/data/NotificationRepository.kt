package com.ansh.awsnotifier.data

class NotificationRepository(private val dao: NotificationDao) {

    suspend fun insert(notification: NotificationEntity) =
        dao.insert(notification)

    suspend fun getAll() = dao.getAll()

    suspend fun getByTopic(topic: String) = dao.getByTopic(topic)

    suspend fun search(query: String) = dao.search(query)

    suspend fun deleteOlderThan(duration: Long) =
        dao.deleteOlderThan(duration)

    suspend fun clearAll() = dao.clearAll()
}

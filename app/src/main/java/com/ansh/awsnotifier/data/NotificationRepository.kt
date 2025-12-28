package com.ansh.awsnotifier.data

import com.ansh.awsnotifier.security.CredentialEncryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepository(private val dao: NotificationDao) {

    suspend fun insert(notification: NotificationEntity) {
        val encryptedMessage = try {
            CredentialEncryption.encrypt(notification.message)
        } catch (e: Exception) {
            notification.message // Fallback
        }
        val entityToSave = notification.copy(message = encryptedMessage)
        dao.insert(entityToSave)
    }

    fun getAll(): Flow<List<NotificationEntity>> {
        return dao.getAll().map { list ->
            list.map { decryptEntity(it) }
        }
    }

    fun getByTopic(topic: String): Flow<List<NotificationEntity>> {
        return dao.getByTopic(topic).map { list ->
            list.map { decryptEntity(it) }
        }
    }

    fun search(query: String): Flow<List<NotificationEntity>> {
        // Search will effectively only work on Title because Message is encrypted
        // The DAO query tries both, but message won't match partial strings
        return dao.search(query).map { list ->
            list.map { decryptEntity(it) }
        }
    }

    suspend fun deleteOlderThan(duration: Long) =
        dao.deleteOlderThan(duration)

    suspend fun clearAll() = dao.clearAll()

    private fun decryptEntity(entity: NotificationEntity): NotificationEntity {
        return try {
            val decryptedMessage = CredentialEncryption.decrypt(entity.message)
            entity.copy(message = decryptedMessage)
        } catch (e: Exception) {
            // If decryption fails (e.g. legacy plain text data), return original
            entity
        }
    }
}

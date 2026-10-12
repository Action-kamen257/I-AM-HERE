package com.iamhere.emergency.data.repository

import com.iamhere.emergency.data.api.NetworkClient
import com.iamhere.emergency.data.model.LocationPoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineLocationQueue {

    private val queue = mutableListOf<LocationPoint>()
    private val mutex = Mutex()

    suspend fun enqueue(point: LocationPoint) {
        mutex.withLock {
            // Buffer up to 500 points (roughly 40+ minutes of 5s updates)
            if (queue.size >= 500) {
                queue.removeAt(0)
            }
            queue.add(point)
        }
    }

    suspend fun flush(token: String): Boolean {
        val pointsToFlush: List<LocationPoint>
        mutex.withLock {
            if (queue.isEmpty()) return true
            pointsToFlush = ArrayList(queue)
        }

        return try {
            val res = NetworkClient.api.updateLocationsBatch(token, mapOf("locations" to pointsToFlush))
            if (res.isSuccessful) {
                mutex.withLock {
                    queue.removeAll(pointsToFlush)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun size(): Int = mutex.withLock { queue.size }

    suspend fun clear() = mutex.withLock { queue.clear() }
}

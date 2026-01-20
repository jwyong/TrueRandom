package com.truerandom.repository

import com.truerandom.dao.PlayCountDao
import com.truerandom.db.entity.PlayCountEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayCountDbRepository @Inject constructor(private val playCountDao: PlayCountDao) {
    suspend fun getAllPlayCounts() = playCountDao.getAllPlayCounts()
    suspend fun getOrphanedPlayCountUris(likedUris: List<String>) = playCountDao.getOrphanedPlayCountUris(likedUris)
    suspend fun upsertPlayCounts(playCounts: List<PlayCountEntity>) = playCountDao.upsertAll(playCounts)
    suspend fun incrementPlayCount(trackUri: String) = playCountDao.incrementPlayCount(trackUri)
    suspend fun deletePlayCountsNotInList(likedUris: List<String>) = playCountDao.deletePlayCountsNotInList(likedUris)
}
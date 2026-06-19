package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val trackId: Long,
    val lastPlayedTimestamp: Long,
    val completionRate: Float,
    val skipCount: Int,
    val playCount: Int
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history WHERE trackId = :trackId")
    suspend fun getHistory(trackId: Long): PlaybackHistoryEntity?

    @Query("SELECT * FROM playback_history ORDER BY playCount DESC LIMIT 50")
    fun getTop50MostPlayed(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE lastPlayedTimestamp >= :fourteenDaysAgo ORDER BY lastPlayedTimestamp DESC")
    fun getRecentlyAdded(fourteenDaysAgo: Long): Flow<List<PlaybackHistoryEntity>>
    
    @Query("UPDATE playback_history SET skipCount = skipCount + 1 WHERE trackId = :trackId")
    suspend fun incrementSkipCount(trackId: Long)
    
    @Query("UPDATE playback_history SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE trackId = :trackId")
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long)
}

@Database(entities = [PlaybackHistoryEntity::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

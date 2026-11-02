package com.eventradar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Something the user cares about, e.g. "WEC", "British Rally Championship", "Arctic Monkeys". */
@Entity(tableName = "interests")
data class Interest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Optional .ics / webcal calendar feed for this interest (e.g. a racing series' season calendar). */
    val feedUrl: String? = null,
    /** For feeds: keep only rounds held in the UK. */
    val ukOnly: Boolean = true,
)

/**
 * One event on the calendar. [id] is a de-duplication key (normalised title + date) so the same
 * gig found by Ticketmaster and by web search shows up once.
 * [interestTags] holds the matching interest names, separated by "|" with leading/trailing "|".
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startDate: String,      // yyyy-MM-dd
    val startTime: String?,     // HH:mm, null if unknown
    val price: String?,
    val venue: String?,
    val city: String?,
    val imageUrl: String?,
    val url: String,
    val source: String,
    val interestTags: String,
    val fetchedAt: Long = System.currentTimeMillis(),
)

val EventEntity.interests: List<String> get() = interestTags.split('|').filter { it.isNotBlank() }
val EventEntity.location: String?
    get() = listOfNotNull(venue, city).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { null }

@Dao
interface InterestDao {
    @Query("SELECT * FROM interests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Interest>>

    @Query("SELECT * FROM interests")
    suspend fun getAll(): List<Interest>

    @Insert
    suspend fun insert(interest: Interest): Long

    @Update
    suspend fun update(interest: Interest)

    @Delete
    suspend fun delete(interest: Interest)
}

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events WHERE startDate >= :today ORDER BY startDate, startTime")
    abstract fun observeUpcoming(today: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    abstract suspend fun getAll(): List<EventEntity>

    @Upsert
    abstract suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id IN (:ids)")
    abstract suspend fun deleteIds(ids: List<String>)

    @Query("DELETE FROM events WHERE startDate < :today")
    abstract suspend fun deletePast(today: String)

    @Query("DELETE FROM events")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replace(deleteIds: List<String>, upserts: List<EventEntity>) {
        if (deleteIds.isNotEmpty()) deleteIds.chunked(500).forEach { deleteIds(it) }
        if (upserts.isNotEmpty()) upsertAll(upserts)
    }
}

@Database(entities = [Interest::class, EventEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun interests(): InterestDao
    abstract fun events(): EventDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "eventradar.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

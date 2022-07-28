package com.example.sampletracksapp.performance

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase

@Dao
abstract class TracksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(track: Track): Long

    @Query("SELECT * FROM Track")
    abstract fun get(): List<Track>
}

@Database(
    entities = [Track::class],
    version = 1,
    exportSchema = false
)
abstract class TracksDatabase : RoomDatabase() {
    abstract fun tracksDao(): TracksDao
}

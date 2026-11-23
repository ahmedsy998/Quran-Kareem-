package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val surahNumber: Int,
    val reciterId: String,
    val updatedAt: Long,
    val configJson: String
)

@Entity(tableName = "favorite_reciters")
data class FavoriteReciterEntity(
    @PrimaryKey val reciterId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestProject(): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT reciterId FROM favorite_reciters")
    fun observeFavoriteReciterIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteReciter(entity: FavoriteReciterEntity)

    @Query("DELETE FROM favorite_reciters WHERE reciterId = :reciterId")
    suspend fun removeFavoriteReciter(reciterId: String)
}

@Database(
    entities = [ProjectEntity::class, FavoriteReciterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quran_video_maker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

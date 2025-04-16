package com.example.chaos2045.database

import android.content.Context
import androidx.room.*
import androidx.room.Entity
import androidx.room.RoomDatabase

@Entity(tableName = "shared_content")
data class SharedContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forwarding_config")
data class ForwardingConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apiUrl: String,
    val token: String,
    val matchString: String
)

@Dao
interface SharedContentDao {
    @Query("SELECT * FROM shared_content ORDER BY timestamp DESC")
    fun getAllContent(): List<SharedContentEntity>

    @Insert
    fun insertContent(content: SharedContentEntity): Long

    @Query("DELETE FROM shared_content WHERE id = :id")
    fun deleteContent(id: Long)

    @Query("SELECT * FROM shared_content WHERE id = :contentId")
    fun getSharedContentById(contentId: Long): SharedContentEntity?
}

@Dao
interface ForwardingConfigDao {
    @Query("SELECT * FROM forwarding_config")
    fun getAllConfigs(): List<ForwardingConfigEntity>

    @Insert
    fun insertConfig(config: ForwardingConfigEntity): Long

    @Delete
    fun deleteConfig(config: ForwardingConfigEntity)

    @Query("SELECT * FROM forwarding_config WHERE id = :configId")
    fun getConfigById(configId: Long): ForwardingConfigEntity?
}

@Database(entities = [SharedContentEntity::class, ForwardingConfigEntity::class], version = 2)
abstract class SharedContentDatabase : RoomDatabase() {
    abstract fun sharedContentDao(): SharedContentDao
    abstract fun forwardingConfigDao(): ForwardingConfigDao

    companion object {
        @Volatile
        private var INSTANCE: SharedContentDatabase? = null

        fun getDatabase(context: Context): SharedContentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SharedContentDatabase::class.java,
                    "shared_content_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

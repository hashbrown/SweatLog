package com.bitfarmsoftware.sweatlog.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.bitfarmsoftware.sweatlog.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "sweatlogs")
data class SweatLog (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    @ColumnInfo(name = "health_connect_id") val connectId: String?,
    @ColumnInfo(name = "workout_data") val workoutData: WorkoutSummary
)

@Dao
interface SweatLogDao {
    @Query("SELECT * FROM sweatlogs")
    fun getAllLogs(): Flow<List<SweatLog>>


    @Insert
    fun insertAll(vararg sweatlogs: SweatLog)

    @Delete
    fun delete(sweatlog: SweatLog)
}

@TypeConverters(SweatLogTypeConverter::class)
@Database(entities = [SweatLog::class], version = 1)
abstract class SweatLogDatabase : RoomDatabase() {
    abstract fun sweatLogDao(): SweatLogDao
}

class SweatLogTypeConverter {
    private val json = Json

   @TypeConverter
    fun convertWorkoutSummaryToString(workoutSummary: WorkoutSummary): String =
        json.encodeToString(workoutSummary)

    @TypeConverter
    fun convertStringToPicEntityList(summaryString: String): WorkoutSummary =
        json.decodeFromString(summaryString)
}
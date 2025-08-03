import androidx.room.*
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY modifiedAt DESC")
    fun getAllProjects(): Flow<List<AudioProject>>
    
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProject(projectId: String): AudioProject?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AudioProject)
    
    @Update
    suspend fun updateProject(project: AudioProject)
    
    @Delete
    suspend fun deleteProject(project: AudioProject)
    
    @Query("UPDATE projects SET isActive = 0")
    suspend fun deactivateAllProjects()
    
    @Query("UPDATE projects SET isActive = 1 WHERE id = :projectId")
    suspend fun setActiveProject(projectId: String)
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackIndex")
    fun getTracksForProject(projectId: String): Flow<List<Track>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)
    
    @Update
    suspend fun updateTrack(track: Track)
    
    @Delete
    suspend fun deleteTrack(track: Track)
    
    @Query("SELECT MAX(trackIndex) FROM tracks WHERE projectId = :projectId")
    suspend fun getMaxTrackIndex(projectId: String): Int?
}

@Dao
interface AudioClipDao {
    @Query("SELECT * FROM audio_clips WHERE trackId = :trackId ORDER BY startTime")
    fun getClipsForTrack(trackId: String): Flow<List<AudioClip>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: AudioClip)
    
    @Update
    suspend fun updateClip(clip: AudioClip)
    
    @Delete
    suspend fun deleteClip(clip: AudioClip)
}

@Database(
    entities = [AudioProject::class, Track::class, AudioClip::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DAWDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun audioClipDao(): AudioClipDao
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}

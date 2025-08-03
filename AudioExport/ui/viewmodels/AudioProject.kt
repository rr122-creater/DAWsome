import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(tableName = "projects")
@Parcelize
data class AudioProject(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val tempo: Float = 120.0f,
    val timeSignature: String = "4/4",
    val projectPath: String,
    val isActive: Boolean = false
) : Parcelable

@Entity(tableName = "tracks")
@Parcelize
data class Track(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val name: String,
    val trackIndex: Int,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val isRecordEnabled: Boolean = false,
    val volume: Float = 1.0f,
    val pan: Float = 0.0f,
    val trackType: TrackType = TrackType.AUDIO,
    val color: Int = 0xFF4CAF50.toInt(),
    val effectsChain: List<String> = emptyList()
) : Parcelable

@Entity(tableName = "audio_clips")
@Parcelize
data class AudioClip(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val name: String,
    val filePath: String,
    val startTime: Long, // in samples
    val duration: Long, // in samples
    val offset: Long = 0L, // clip start offset
    val gain: Float = 1.0f,
    val fadeIn: Long = 0L,
    val fadeOut: Long = 0L,
    val isLooped: Boolean = false
) : Parcelable

enum class TrackType {
    AUDIO, MIDI, INSTRUMENT, BUS
}

enum class PlaybackState {
    STOPPED, PLAYING, RECORDING, PAUSED
}

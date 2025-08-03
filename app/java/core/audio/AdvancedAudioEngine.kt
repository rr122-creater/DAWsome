import android.content.Context
import android.media.*
import android.os.Process
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedAudioEngine @Inject constructor(
    private val context: Context
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 2
        const val BUFFER_SIZE_FRAMES = 256 // Low latency
        const val BYTES_PER_SAMPLE = 4 // 32-bit float
    }

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private val isEngineRunning = AtomicBoolean(false)
    private val playbackState = AtomicReference(PlaybackState.STOPPED)
    
    // Audio processing thread
    private var audioThread: Thread? = null
    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Audio buffers
    private val inputBuffer = FloatArray(BUFFER_SIZE_FRAMES * CHANNELS)
    private val outputBuffer = FloatArray(BUFFER_SIZE_FRAMES * CHANNELS)
    private val mixBuffer = FloatArray(BUFFER_SIZE_FRAMES * CHANNELS)
    
    // Track management
    private val activeTracks = mutableMapOf<String, TrackProcessor>()
    private var masterVolume = 1.0f
    
    // Performance monitoring
    private var cpuUsage = 0.0
    private var bufferUnderruns = 0

    fun initialize(): Boolean {
        return try {
            setupAudioTrack()
            setupAudioRecord()
            true
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to initialize audio engine", e)
            false
        }
    }

    private fun setupAudioTrack() {
        val bufferSizeBytes = BUFFER_SIZE_FRAMES * CHANNELS * BYTES_PER_SAMPLE
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun setupAudioRecord() {
        val bufferSizeBytes = BUFFER_SIZE_FRAMES * CHANNELS * BYTES_PER_SAMPLE
        
        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .build()
    }

    fun startEngine() {
        if (isEngineRunning.get()) return
        
        isEngineRunning.set(true)
        audioTrack?.play()
        audioRecord?.startRecording()
        
        startAudioThread()
    }

    private fun startAudioThread() {
        audioThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            audioProcessingLoop()
        }, "AudioEngine").apply {
            start()
        }
    }

    private fun audioProcessingLoop() {
        val startTime = System.nanoTime()
        var frameCount = 0L
        
        while (isEngineRunning.get()) {
            val cycleStart = System.nanoTime()
            
            try {
                // Clear mix buffer
                mixBuffer.fill(0.0f)
                
                // Record input if recording
                if (playbackState.get() == PlaybackState.RECORDING) {
                    audioRecord?.read(inputBuffer, 0, inputBuffer.size, AudioRecord.READ_BLOCKING)
                }
                
                // Process all active tracks
                processActiveTracks()
                
                // Apply master effects and volume
                applyMasterProcessing()
                
                // Output to audio track
                audioTrack?.write(outputBuffer, 0, outputBuffer.size, AudioTrack.WRITE_BLOCKING)
                
                frameCount++
                
                // Monitor performance every second
                if (frameCount % (SAMPLE_RATE / BUFFER_SIZE_FRAMES) == 0L) {
                    updatePerformanceMetrics(cycleStart, startTime, frameCount)
                }
                
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error in audio processing loop", e)
                bufferUnderruns++
            }
        }
    }

    private fun processActiveTracks() {
        activeTracks.values.forEach { trackProcessor ->
            if (!trackProcessor.track.isMuted && 
                (activeTracks.values.none { it.track.isSolo } || trackProcessor.track.isSolo)) {
                
                trackProcessor.process(inputBuffer, mixBuffer, BUFFER_SIZE_FRAMES)
            }
        }
    }

    private fun applyMasterProcessing() {
        for (i in mixBuffer.indices) {
            outputBuffer[i] = (mixBuffer[i] * masterVolume).coerceIn(-1.0f, 1.0f)
        }
    }

    private fun updatePerformanceMetrics(cycleStart: Long, startTime: Long, frameCount: Long) {
        val cycleTime = (System.nanoTime() - cycleStart) / 1_000_000.0
        val maxCycleTime = (BUFFER_SIZE_FRAMES * 1000.0) / SAMPLE_RATE
        cpuUsage = (cycleTime / maxCycleTime) * 100.0
        
        Log.d("AudioEngine", "CPU Usage: ${cpuUsage.toInt()}%, Buffer underruns: $bufferUnderruns")
    }

    fun addTrack(track: Track): TrackProcessor {
        val processor = TrackProcessor(track, SAMPLE_RATE, BUFFER_SIZE_FRAMES)
        activeTracks[track.id] = processor
        return processor
    }

    fun removeTrack(trackId: String) {
        activeTracks.remove(trackId)?.release()
    }

    fun setPlaybackState(state: PlaybackState) {
        playbackState.set(state)
    }

    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(cpuUsage, bufferUnderruns, activeTracks.size)
    }

    fun release() {
        isEngineRunning.set(false)
        audioThread?.join(1000)
        
        audioTrack?.stop()
        audioTrack?.release()
        audioRecord?.stop()
        audioRecord?.release()
        
        activeTracks.values.forEach { it.release() }
        activeTracks.clear()
        
        audioScope.cancel()
    }
}

data class PerformanceMetrics(
    val cpuUsage: Double,
    val bufferUnderruns: Int,
    val activeTrackCount: Int
)

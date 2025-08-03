// core/audio/RecordingManager.kt
@Singleton
class RecordingManager @Inject constructor(
    private val context: Context,
    private val audioFileManager: AudioFileManager
) {
    private var audioRecord: AudioRecord? = null
    private var recordingFile: File? = null
    private var isRecording = false
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    fun startRecording(projectPath: String, trackName: String): Boolean {
        if (isRecording) return false
        
        return try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            // Create recording file
            val timestamp = System.currentTimeMillis()
            recordingFile = File(projectPath, "audio/${trackName}_$timestamp.wav")
            
            audioRecord?.startRecording()
            isRecording = true
            
            // Start recording in background
            recordingScope.launch {
                writeAudioDataToFile()
            }
            
            true
        } catch (e: SecurityException) {
            Log.e("RecordingManager", "Permission denied for recording", e)
            false
        } catch (e: Exception) {
            Log.e("RecordingManager", "Failed to start recording", e)
            false
        }
    }
    
    fun stopRecording(): File? {
        if (!isRecording) return null
        
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        return recordingFile
    }
    
    private suspend fun writeAudioDataToFile() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        val audioData = ShortArray(bufferSize)
        
        recordingFile?.let { file ->
            FileOutputStream(file).use { fos ->
                // Write WAV header (simplified)
                writeWavHeader(fos, SAMPLE_RATE, 2, 16)
                
                while (isRecording) {
                    val readResult = audioRecord?.read(audioData, 0, bufferSize) ?: 0
                    if (readResult > 0) {
                        // Convert shorts to bytes and write
                        val byteData = ByteArray(readResult * 2)
                        for (i in 0 until readResult) {
                            val sample = audioData[i]
                            byteData[i * 2] = (sample.toInt() and 0xff).toByte()
                            byteData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xff).toByte()
                        }
                        fos.write(byteData)
                    }
                }
            }
        }
    }
    
    private fun writeWavHeader(
        fos: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        
        // WAV header (44 bytes)
        fos.write("RIFF".toByteArray())
        fos.write(intToByteArray(36)) // ChunkSize (will be updated later)
        fos.write("WAVE".toByteArray())
        fos.write("fmt ".toByteArray())
        fos.write(intToByteArray(16)) // Subchunk1Size
        fos.write(shortToByteArray(1)) // AudioFormat (PCM)
        fos.write(shortToByteArray(channels.toShort()))
        fos.write(intToByteArray(sampleRate))
        fos.write(intToByteArray(byteRate))
        fos.write(shortToByteArray(blockAlign.toShort()))
        fos.write(shortToByteArray(bitsPerSample.toShort()))
        fos.write("data".toByteArray())
        fos.write(intToByteArray(0)) // Subchunk2Size (will be updated later)
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 

// core/audio/AudioEngineManager.kt
@Singleton
class AudioEngineManager @Inject constructor(
    private val context: Context,
    private val projectRepository: ProjectRepository
) {
    private val nativeEngine = NativeAudioEngine()
    private var currentProject: ProjectData? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState = _playbackState.asStateFlow()
    
    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition = _playbackPosition.asStateFlow()
    
    private val _cpuUsage = MutableStateFlow(0.0)
    val cpuUsage = _cpuUsage.asStateFlow()
    
    suspend fun loadProject(projectId: String): Boolean {
        return try {
            currentProject = projectRepository.loadProject(projectId)
            currentProject?.let { projectData ->
                nativeEngine.clearAllTracks()
                projectData.tracks.forEach { track ->
                    val nativeTrackId = nativeEngine.addTrack()
                    nativeEngine.setTrackProperties(
                        nativeTrackId,
                        track.volume,
                        track.pan,
                        track.isMuted,
                        track.isSolo
                    )
                    
                    // Load audio clips for this track
                    val trackClips = projectData.clips.filter { it.trackId == track.id }
                    trackClips.forEach { clip ->
                        nativeEngine.addAudioClip(
                            nativeTrackId,
                            clip.filePath,
                            clip.startTime,
                            clip.duration,
                            clip.offset,
                            clip.gain
                        )
                    }
                }
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to load project", e)
            false
        }
    }
    
    fun play() {
        nativeEngine.startPlayback()
        _playbackState.value = PlaybackState.PLAYING
        startPositionUpdates()
    }
    
    fun stop() {
        nativeEngine.stopPlayback()
        _playbackState.value = PlaybackState.STOPPED
        stopPositionUpdates()
    }
    
    fun record() {
        nativeEngine.startRecording()
        _playbackState.value = PlaybackState.RECORDING
        startPositionUpdates()
    }
    
    private fun startPositionUpdates() {
        // Update playback position every 50ms
        CoroutineScope(Dispatchers.Default).launch {
            while (_playbackState.value != PlaybackState.STOPPED) {
                _playbackPosition.value = nativeEngine.getPlaybackPosition()
                _cpuUsage.value = nativeEngine.getCpuUsage()
                delay(50)
            }
        }
    }
}

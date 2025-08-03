// ui/viewmodels/DAWViewModel.kt
@HiltViewModel
class DAWViewModel @Inject constructor(
    private val audioEngineManager: AudioEngineManager,
    private val projectRepository: ProjectRepository,
    private val trackRepository: TrackRepository
) : ViewModel() {

    private val _currentProject = MutableStateFlow<AudioProject?>(null)
    val currentProject = _currentProject.asStateFlow()
    
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks = _tracks.asStateFlow()
    
    private val _selectedTrackId = MutableStateFlow<String?>(null)
    val selectedTrackId = _selectedTrackId.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    // Audio engine states
    val playbackState = audioEngineManager.playbackState
    val playbackPosition = audioEngineManager.playbackPosition
    val cpuUsage = audioEngineManager.cpuUsage
    
    init {
        // Observe track changes for current project
        viewModelScope.launch {
            currentProject.collect { project ->
                project?.let { 
                    trackRepository.getTracksForProject(it.id).collect { trackList ->
                        _tracks.value = trackList
                    }
                }
            }
        }
    }
    
    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = audioEngineManager.loadProject(projectId)
                if (success) {
                    val project = projectRepository.getProject(projectId)
                    _currentProject.value = project
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to load project"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createNewProject(name: String, sampleRate: Int = 44100) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = projectRepository.createProject(name, sampleRate)
                _currentProject.value = project
                
                // Create default audio track
                addTrack("Audio 1", TrackType.AUDIO)
                
                // Load into audio engine
                audioEngineManager.loadProject(project.id)
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Transport Controls
    fun play() = audioEngineManager.play()
    fun stop() = audioEngineManager.stop()
    fun record() = audioEngineManager.record()
    fun pause() = audioEngineManager.pause()
    
    fun seekTo(position: Long) {
        audioEngineManager.seekTo(position)
    }
    
    // Track Management
    fun addTrack(name: String = "New Track", type: TrackType = TrackType.AUDIO) {
        viewModelScope.launch {
            currentProject.value?.let { project ->
                val trackIndex = _tracks.value.size
                val track = Track(
                    projectId = project.id,
                    name = name,
                    trackIndex = trackIndex,
                    trackType = type,
                    color = getRandomTrackColor()
                )
                
                trackRepository.insertTrack(track)
                audioEngineManager.addTrack(track)
            }
        }
    }
    
    fun deleteTrack(trackId: String) {
        viewModelScope.launch {
            trackRepository.deleteTrack(trackId)
            audioEngineManager.removeTrack(trackId)
        }
    }
    
    fun setTrackVolume(trackId: String, volume: Float) {
        viewModelScope.launch {
            trackRepository.updateTrackVolume(trackId, volume)
            audioEngineManager.setTrackVolume(trackId, volume)
        }
    }
    
    fun setTrackPan(trackId: String, pan: Float) {
        viewModelScope.launch {
            trackRepository.updateTrackPan(trackId, pan)
            audioEngineManager.setTrackPan(trackId, pan)
        }
    }
    
    fun toggleTrackMute(trackId: String) {
        viewModelScope.launch {
            val track = _tracks.value.find { it.id == trackId } ?: return@launch
            val newMuteState = !track.isMuted
            trackRepository.updateTrackMute(trackId, newMuteState)
            audioEngineManager.setTrackMute(trackId, newMuteState)
        }
    }
    
    fun toggleTrackSolo(trackId: String) {
        viewModelScope.launch {
            val track = _tracks.value.find { it.id == trackId } ?: return@launch
            val newSoloState = !track.isSolo
            trackRepository.updateTrackSolo(trackId, newSoloState)
            audioEngineManager.setTrackSolo(trackId, newSoloState)
        }
    }
    
    private fun getRandomTrackColor(): Int {
        val colors = listOf(
            0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFFE91E63,
            0xFF9C27B0, 0xFF00BCD4, 0xFFFFEB3B, 0xFFFF5722
        )
        return colors.random().toInt()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

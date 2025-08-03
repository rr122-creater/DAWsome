class NativeAudioEngine {
    companion object {
        init {
            System.loadLibrary("mobiledaw")
        }
    }
    
    external fun initialize(): Boolean
    external fun startPlayback()
    external fun stopPlayback()
    external fun startRecording()
    external fun stopRecording()
    external fun addTrack(): Int
    external fun removeTrack(trackId: Int)
    external fun setTrackVolume(trackId: Int, volume: Float)
    external fun setTrackPan(trackId: Int, pan: Float)
    external fun setTrackMute(trackId: Int, muted: Boolean)
    external fun setMasterVolume(volume: Float)
}

@Singleton
class AudioEngineManager @Inject constructor() {
    private val nativeEngine = NativeAudioEngine()
    private val _playbackState = MutableLiveData(PlaybackState.STOPPED)
    val playbackState: LiveData<PlaybackState> = _playbackState
    
    fun initialize(): Boolean {
        return nativeEngine.initialize()
    }
    
    fun play() {
        nativeEngine.startPlayback()
        _playbackState.value = PlaybackState.PLAYING
    }
    
    fun stop() {
        nativeEngine.stopPlayback()
        _playbackState.value = PlaybackState.STOPPED
    }
    
    fun record() {
        nativeEngine.startRecording()
        _playbackState.value = PlaybackState.RECORDING
    }
}

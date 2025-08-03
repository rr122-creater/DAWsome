import kotlin.math.*

class TrackProcessor(
    val track: Track,
    private val sampleRate: Int,
    private val bufferSize: Int
) {
    private val effectsChain = mutableListOf<AudioEffect>()
    private val trackBuffer = FloatArray(bufferSize * 2) // Stereo
    private var currentClipIndex = 0
    private var playbackPosition = 0L // in samples
    
    // Built-in effects
    private val eq3Band = EQ3Band(sampleRate)
    private val compressor = Compressor(sampleRate)
    private val reverb = SimpleReverb(sampleRate)
    
    init {
        setupDefaultEffects()
    }

    private fun setupDefaultEffects() {
        // Add default EQ and compressor
        effectsChain.add(eq3Band)
        effectsChain.add(compressor)
        

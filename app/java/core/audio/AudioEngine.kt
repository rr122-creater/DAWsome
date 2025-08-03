import android.content.Context
import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngine @Inject constructor(
    private val context: Context
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 2

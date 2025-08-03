#include <jni.h>
#include <oboe/Oboe.h>
#include <vector>
#include <memory>
#include <atomic>

class MobileDAWEngine : public oboe::AudioStreamDataCallback {
private:
    std::unique_ptr<oboe::AudioStream> mPlayStream;
    std::unique_ptr<oboe::AudioStream> mRecordStream;
    std::atomic<bool> mIsRecording{false};
    std::atomic<bool> mIsPlaying{false};
    
    // Audio buffers
    std::vector<float> mMixBuffer;
    std::vector<float> mRecordBuffer;
    
    // Track management
    struct Track {
        std::vector<float> buffer;
        float volume = 1.0f;
        float pan = 0.0f;
        bool muted = false;
        bool solo = false;
    };
    
    std::vector<std::unique_ptr<Track>> mTracks;
    float mMasterVolume = 1.0f;
    
public:
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int32_t numFrames) override {
        
        float *outputData = static_cast<float *>(audioData);
        
        // Clear output buffer
        std::fill(outputData, outputData + numFrames * 2, 0.0f);
        
        // Mix all active tracks
        for (auto& track : mTracks) {
            if (!track->muted && (getSoloTracks() == 0 || track->solo)) {
                mixTrack(*track, outputData, numFrames);
            }
        }
        
        // Apply master volume
        for (int i = 0; i < numFrames * 2; ++i) {
            outputData[i] *= mMasterVolume;
            // Soft clipping
            outputData[i] = std::tanh(outputData[i]);
        }
        
        return oboe::DataCallbackResult::Continue;
    }
    
    bool initialize() {
        // Setup playback stream
        oboe::AudioStreamBuilder playBuilder;
        playBuilder.setDirection(oboe::Direction::Output)
                   ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                   ->setFormat(oboe::AudioFormat::Float)
                   ->setChannelCount(2)
                   ->setSampleRate(44100)
                   ->setDataCallback(this)
                   ->setFramesPerDataCallback(256);
        
        oboe::Result result = playBuilder.openStream(mPlayStream);
        return result == oboe::Result::OK;
    }
    
    void startPlayback() {
        if (mPlayStream) {
            mIsPlaying = true;
            mPlayStream->requestStart();
        }
    }
    
    void stopPlayback() {
        if (mPlayStream) {
            mIsPlaying = false;
            mPlayStream->requestStop();
        }
    }
    
private:
    void mixTrack(const Track& track, float* output, int32_t numFrames) {
        // Simplified mixing - in production, this would be much more complex
        for (int i = 0; i < numFrames * 2; i += 2) {
            float sample = track.buffer[i / 2] * track.volume;
            
            // Pan calculation
            float leftGain = (1.0f - std::max(0.0f, track.pan)) * 0.707f;
            float rightGain = (1.0f + std::min(0.0f, track.pan)) * 0.707f;
            
            output[i] += sample * leftGain;     // Left
            output[i + 1] += sample * rightGain; // Right
        }
    }
    
    int getSoloTracks() {
        int count = 0;
        for (const auto& track : mTracks) {
            if (track->solo) count++;
        }
        return count;
    }
};

// JNI bindings
extern "C" {
    static MobileDAWEngine* engine = nullptr;
    
    JNIEXPORT jboolean JNICALL
    Java_com_mobiledaw_pro_core_audio_NativeAudioEngine_initialize(JNIEnv *env, jobject thiz) {
        if (!engine) {
            engine = new MobileDAWEngine();
        }
        return engine->initialize();
    }
    
    JNIEXPORT void JNICALL
    Java_com_mobiledaw_pro_core_audio_NativeAudioEngine_startPlayback(JNIEnv *env, jobject thiz) {
        if (engine) {
            engine->startPlayback();
        }
    }
    
    JNIEXPORT void JNICALL
    Java_com_mobiledaw_pro_core_audio_NativeAudioEngine_stopPlayback(JNIEnv *env, jobject thiz) {
        if (engine) {
            engine->stopPlayback();
        }
    }
}

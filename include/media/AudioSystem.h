/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_AUDIOSYSTEM_H_
#define ANDROID_AUDIOSYSTEM_H_

#include <utils/RefBase.h>
#include <utils/threads.h>
#include <media/IAudioFlinger.h>

#include <system/audio.h>
#include <system/audio_policy.h>

/* XXX: Should be include by all the users instead */
#include <media/AudioParameter.h>

namespace android {

typedef void (*audio_error_callback)(status_t err);

class IAudioPolicyService;
class String8;

class AudioSystem
{
public:
#ifdef BOARD_USES_AUDIO_LEGACY

    enum audio_devices {
        // output devices
        DEVICE_OUT_EARPIECE = 0x1,
        DEVICE_OUT_SPEAKER = 0x2,
        DEVICE_OUT_WIRED_HEADSET = 0x4,
        DEVICE_OUT_WIRED_HEADPHONE = 0x8,
        DEVICE_OUT_BLUETOOTH_SCO = 0x10,
        DEVICE_OUT_BLUETOOTH_SCO_HEADSET = 0x20,
        DEVICE_OUT_BLUETOOTH_SCO_CARKIT = 0x40,
        DEVICE_OUT_BLUETOOTH_A2DP = 0x80,
        DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES = 0x100,
        DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER = 0x200,
        DEVICE_OUT_AUX_DIGITAL = 0x400,
#ifdef HAVE_FM_RADIO
        DEVICE_OUT_FM = 0x800,
        DEVICE_OUT_FM_SPEAKER = 0x1000,
        DEVICE_OUT_FM_ALL = (DEVICE_OUT_FM | DEVICE_OUT_FM_SPEAKER),
#elif defined(OMAP_ENHANCEMENT)
        DEVICE_OUT_FM_TRANSMIT = 0x800,
        DEVICE_OUT_LOW_POWER = 0x1000,
#endif
        DEVICE_OUT_HDMI = 0x2000,
        DEVICE_OUT_DEFAULT = 0x8000,
        DEVICE_OUT_ALL = (DEVICE_OUT_EARPIECE | DEVICE_OUT_SPEAKER | DEVICE_OUT_WIRED_HEADSET |
#ifdef HAVE_FM_RADIO
                DEVICE_OUT_WIRED_HEADPHONE | DEVICE_OUT_FM | DEVICE_OUT_FM_SPEAKER | DEVICE_OUT_BLUETOOTH_SCO | DEVICE_OUT_BLUETOOTH_SCO_HEADSET |
#else
                DEVICE_OUT_WIRED_HEADPHONE | DEVICE_OUT_BLUETOOTH_SCO | DEVICE_OUT_BLUETOOTH_SCO_HEADSET |
#endif
                DEVICE_OUT_BLUETOOTH_SCO_CARKIT | DEVICE_OUT_BLUETOOTH_A2DP | DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES |
#if defined(OMAP_ENHANCEMENT) && !defined(HAVE_FM_RADIO)
                DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER | DEVICE_OUT_AUX_DIGITAL | DEVICE_OUT_LOW_POWER |
                DEVICE_OUT_FM_TRANSMIT | DEVICE_OUT_DEFAULT),
#else
                DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER | DEVICE_OUT_AUX_DIGITAL | DEVICE_OUT_HDMI | DEVICE_OUT_DEFAULT),
#endif
        DEVICE_OUT_ALL_A2DP = (DEVICE_OUT_BLUETOOTH_A2DP | DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES |
                DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER),

        // input devices
        DEVICE_IN_COMMUNICATION = 0x10000,
        DEVICE_IN_AMBIENT = 0x20000,
        DEVICE_IN_BUILTIN_MIC = 0x40000,
        DEVICE_IN_BLUETOOTH_SCO_HEADSET = 0x80000,
        DEVICE_IN_WIRED_HEADSET = 0x100000,
        DEVICE_IN_AUX_DIGITAL = 0x200000,
        DEVICE_IN_VOICE_CALL = 0x400000,
        DEVICE_IN_BACK_MIC = 0x800000,
#ifdef HAVE_FM_RADIO
        DEVICE_IN_FM_RX = 0x1000000,
        DEVICE_IN_FM_RX_A2DP = 0x2000000,
#endif
#ifdef OMAP_ENHANCEMENT
        DEVICE_IN_FM_ANALOG = 0x1000000,
#endif
        DEVICE_IN_DEFAULT = 0x80000000,

        DEVICE_IN_ALL = (DEVICE_IN_COMMUNICATION | DEVICE_IN_AMBIENT | DEVICE_IN_BUILTIN_MIC |
                DEVICE_IN_BLUETOOTH_SCO_HEADSET | DEVICE_IN_WIRED_HEADSET | DEVICE_IN_AUX_DIGITAL |
#ifdef HAVE_FM_RADIO
                DEVICE_IN_VOICE_CALL | DEVICE_IN_BACK_MIC | DEVICE_IN_FM_RX | DEVICE_IN_FM_RX_A2DP | DEVICE_IN_DEFAULT)
#elif OMAP_ENHANCEMENT
                DEVICE_IN_VOICE_CALL | DEVICE_IN_BACK_MIC  | DEVICE_IN_FM_ANALOG | DEVICE_IN_DEFAULT)
#else
                DEVICE_IN_VOICE_CALL | DEVICE_IN_BACK_MIC | DEVICE_IN_DEFAULT)
#endif
    };

    // device connection states used for setDeviceConnectionState()
    enum device_connection_state {
        DEVICE_STATE_UNAVAILABLE,
        DEVICE_STATE_AVAILABLE,
        NUM_DEVICE_STATES
    };

    enum stream_type {
        DEFAULT          =-1,
        VOICE_CALL       = 0,
        SYSTEM           = 1,
        RING             = 2,
        MUSIC            = 3,
        ALARM            = 4,
        NOTIFICATION     = 5,
        BLUETOOTH_SCO    = 6,
        ENFORCED_AUDIBLE = 7, // Sounds that cannot be muted by user and must be routed to speaker
        DTMF             = 8,
        TTS              = 9,
#ifdef HAVE_FM_RADIO
        FM              = 10,
#endif
        NUM_STREAM_TYPES
    };

    enum audio_in_acoustics {
        AGC_ENABLE    = 0x0001,
        AGC_DISABLE   = 0,
        NS_ENABLE     = 0x0002,
        NS_DISABLE    = 0,
        TX_IIR_ENABLE = 0x0004,
        TX_DISABLE    = 0
    };

#endif
    /* These are static methods to control the system-wide AudioFlinger
     * only privileged processes can have access to them
     */

    // mute/unmute microphone
    static status_t muteMicrophone(bool state);
    static status_t isMicrophoneMuted(bool *state);

    // set/get master volume
    static status_t setMasterVolume(float value);
    static status_t getMasterVolume(float* volume);

    // mute/unmute audio outputs
    static status_t setMasterMute(bool mute);
    static status_t getMasterMute(bool* mute);

    // set/get stream volume on specified output
    static status_t setStreamVolume(int stream, float value, int output);
    static status_t getStreamVolume(int stream, float* volume, int output);

    // mute/unmute stream
    static status_t setStreamMute(int stream, bool mute);
    static status_t getStreamMute(int stream, bool* mute);

    // set audio mode in audio hardware (see audio_mode_t)
    static status_t setMode(int mode);

    // returns true in *state if tracks are active on the specified stream or has been active
    // in the past inPastMs milliseconds
    static status_t isStreamActive(int stream, bool *state, uint32_t inPastMs = 0);

    // set/get audio hardware parameters. The function accepts a list of parameters
    // key value pairs in the form: key1=value1;key2=value2;...
    // Some keys are reserved for standard parameters (See AudioParameter class).
    static status_t setParameters(audio_io_handle_t ioHandle, const String8& keyValuePairs);
    static String8  getParameters(audio_io_handle_t ioHandle, const String8& keys);

    static void setErrorCallback(audio_error_callback cb);

    // helper function to obtain AudioFlinger service handle
    static const sp<IAudioFlinger>& get_audio_flinger();

    static float linearToLog(int volume);
    static int logToLinear(float volume);

    static status_t getOutputSamplingRate(int* samplingRate, int stream = AUDIO_STREAM_DEFAULT);
    static status_t getOutputFrameCount(int* frameCount, int stream = AUDIO_STREAM_DEFAULT);
    static status_t getOutputLatency(uint32_t* latency, int stream = AUDIO_STREAM_DEFAULT);

    static bool routedToA2dpOutput(int streamType);

    static status_t getInputBufferSize(uint32_t sampleRate, int format, int channelCount,
        size_t* buffSize);

    static status_t setVoiceVolume(float volume);

    // return the number of audio frames written by AudioFlinger to audio HAL and
    // audio dsp to DAC since the output on which the specified stream is playing
    // has exited standby.
    // returned status (from utils/Errors.h) can be:
    // - NO_ERROR: successful operation, halFrames and dspFrames point to valid data
    // - INVALID_OPERATION: Not supported on current hardware platform
    // - BAD_VALUE: invalid parameter
    // NOTE: this feature is not supported on all hardware platforms and it is
    // necessary to check returned status before using the returned values.
    static status_t getRenderPosition(uint32_t *halFrames, uint32_t *dspFrames, int stream = AUDIO_STREAM_DEFAULT);

    static unsigned int  getInputFramesLost(audio_io_handle_t ioHandle);

    static int newAudioSessionId();
    static void acquireAudioSessionId(int audioSession);
    static void releaseAudioSessionId(int audioSession);

    // types of io configuration change events received with ioConfigChanged()
    enum io_config_event {
        OUTPUT_OPENED,
        OUTPUT_CLOSED,
        OUTPUT_CONFIG_CHANGED,
        INPUT_OPENED,
        INPUT_CLOSED,
        INPUT_CONFIG_CHANGED,
        STREAM_CONFIG_CHANGED,
        NUM_CONFIG_EVENTS
    };

    // audio output descritor used to cache output configurations in client process to avoid frequent calls
    // through IAudioFlinger
    class OutputDescriptor {
    public:
        OutputDescriptor()
        : samplingRate(0), format(0), channels(0), frameCount(0), latency(0)  {}

        uint32_t samplingRate;
        int32_t format;
        int32_t channels;
        size_t frameCount;
        uint32_t latency;
    };

    //
    // IAudioPolicyService interface (see AudioPolicyInterface for method descriptions)
    //
    static status_t setDeviceConnectionState(audio_devices_t device, audio_policy_dev_state_t state, const char *device_address);
    static audio_policy_dev_state_t getDeviceConnectionState(audio_devices_t device, const char *device_address);
    static status_t setPhoneState(int state);
    static status_t setRingerMode(uint32_t mode, uint32_t mask);
    static status_t setForceUse(audio_policy_force_use_t usage, audio_policy_forced_cfg_t config);
    static audio_policy_forced_cfg_t getForceUse(audio_policy_force_use_t usage);
    static audio_io_handle_t getOutput(audio_stream_type_t stream,
                                        uint32_t samplingRate = 0,
                                        uint32_t format = AUDIO_FORMAT_DEFAULT,
                                        uint32_t channels = AUDIO_CHANNEL_OUT_STEREO,
                                        audio_policy_output_flags_t flags = AUDIO_POLICY_OUTPUT_FLAG_INDIRECT);
    static status_t startOutput(audio_io_handle_t output,
                                audio_stream_type_t stream,
                                int session = 0);
    static status_t stopOutput(audio_io_handle_t output,
                               audio_stream_type_t stream,
                               int session = 0);
    static void releaseOutput(audio_io_handle_t output);
    static audio_io_handle_t getInput(int inputSource,
                                    uint32_t samplingRate = 0,
                                    uint32_t format = AUDIO_FORMAT_DEFAULT,
                                    uint32_t channels = AUDIO_CHANNEL_IN_MONO,
                                    audio_in_acoustics_t acoustics = (audio_in_acoustics_t)0,
                                    int sessionId = 0);
    static status_t startInput(audio_io_handle_t input);
    static status_t stopInput(audio_io_handle_t input);
    static void releaseInput(audio_io_handle_t input);
    static status_t initStreamVolume(audio_stream_type_t stream,
                                      int indexMin,
                                      int indexMax);
    static status_t setStreamVolumeIndex(audio_stream_type_t stream, int index);
    static status_t getStreamVolumeIndex(audio_stream_type_t stream, int *index);

    static uint32_t getStrategyForStream(audio_stream_type_t stream);
    static uint32_t getDevicesForStream(audio_stream_type_t stream);

    static audio_io_handle_t getOutputForEffect(effect_descriptor_t *desc);
    static status_t registerEffect(effect_descriptor_t *desc,
                                    audio_io_handle_t io,
                                    uint32_t strategy,
                                    int session,
                                    int id);
    static status_t unregisterEffect(int id);
    static status_t setEffectEnabled(int id, bool enabled);

    // clear stream to output mapping cache (gStreamOutputMap)
    // and output configuration cache (gOutputs)
    static void clearAudioConfigCache();

    static const sp<IAudioPolicyService>& get_audio_policy_service();
#ifdef BOARD_USES_AUDIO_LEGACY
    static uint32_t popCount(uint32_t u);
    static bool isOutputDevice(audio_devices device);
    static bool isInputDevice(audio_devices device);
#ifdef HAVE_FM_RADIO
    static bool isFmDevice(audio_devices device);
#endif
    static bool isA2dpDevice(audio_devices device);
    static bool isBluetoothScoDevice(audio_devices device);
    static bool isLowVisibility(stream_type stream);
    static bool isInputChannel(uint32_t channel);
    static bool isOutputChannel(uint32_t channel);
    static bool isValidFormat(uint32_t format);
    static bool isLinearPCM(uint32_t format);
    static status_t setDeviceConnectionState(audio_devices device, device_connection_state state, const char *device_address);
    static device_connection_state getDeviceConnectionState(audio_devices device, const char *device_address);
#endif

    // ----------------------------------------------------------------------------

private:

    class AudioFlingerClient: public IBinder::DeathRecipient, public BnAudioFlingerClient
    {
    public:
        AudioFlingerClient() {
        }

        // DeathRecipient
        virtual void binderDied(const wp<IBinder>& who);

        // IAudioFlingerClient

        // indicate a change in the configuration of an output or input: keeps the cached
        // values for output/input parameters upto date in client process
        virtual void ioConfigChanged(int event, int ioHandle, void *param2);
    };

    class AudioPolicyServiceClient: public IBinder::DeathRecipient
    {
    public:
        AudioPolicyServiceClient() {
        }

        // DeathRecipient
        virtual void binderDied(const wp<IBinder>& who);
    };

    static sp<AudioFlingerClient> gAudioFlingerClient;
    static sp<AudioPolicyServiceClient> gAudioPolicyServiceClient;
    friend class AudioFlingerClient;
    friend class AudioPolicyServiceClient;

    static Mutex gLock;
    static sp<IAudioFlinger> gAudioFlinger;
    static audio_error_callback gAudioErrorCallback;

    static size_t gInBuffSize;
    // previous parameters for recording buffer size queries
    static uint32_t gPrevInSamplingRate;
    static int gPrevInFormat;
    static int gPrevInChannelCount;

    static sp<IAudioPolicyService> gAudioPolicyService;

    // mapping between stream types and outputs
    static DefaultKeyedVector<int, audio_io_handle_t> gStreamOutputMap;
    // list of output descriptors containing cached parameters
    // (sampling rate, framecount, channel count...)
    static DefaultKeyedVector<audio_io_handle_t, OutputDescriptor *> gOutputs;
};

};  // namespace android

#endif  /*ANDROID_AUDIOSYSTEM_H_*/

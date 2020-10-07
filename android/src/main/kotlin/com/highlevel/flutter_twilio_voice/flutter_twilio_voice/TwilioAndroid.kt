package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import com.highlevel.flutter_twilio_voice.flutter_twilio_voice.AppRtc.AppRTCAudioManager
import com.twilio.voice.*
import io.flutter.plugin.common.MethodChannel


class TwilioAndroid(context: Context,
                    wakeLock: PowerManager.WakeLock,
                    audioManager: AppRTCAudioManager,
                    channel: MethodChannel,
                    val cancelNotification: () -> Unit) {
    private val TAG = "FlutterTwilio"
    private val _audioManager: AppRTCAudioManager = audioManager
    private var savedAudioMode = AudioManager.MODE_INVALID
    private var params: HashMap<String, String> = HashMap<String, String>()
    private var activeCall: Call? = null
    private val callListener: Call.Listener = callListener()
    private val _wakeLock: PowerManager.WakeLock = wakeLock
    private val _context: Context = context
    val _channel: MethodChannel = channel

    init {
        init()
    }

    fun init() {
        _audioManager.start { audioDevice, availableAudioDevices ->
            // This method will be called each time the number of available audio
            // devices has changed.
            onAudioManagerDevicesChanged(audioDevice, availableAudioDevices)
        }
    }

    private fun onAudioManagerDevicesChanged(
            device: AppRTCAudioManager.AudioDevice, availableDevices: Set<AppRTCAudioManager.AudioDevice>) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected device: " + device)
        // TODO(henrika): add callback handler.
        val args = HashMap<String, String>()
        args.put("devices", availableDevices.toString())
        args.put("selectedDevice", device.toString())
        args.put("status","deviceUpdate")
        _channel.invokeMethod("call_listener", args)
    }

    fun callListener(): Call.Listener {
        return object : Call.Listener {
            override fun onRinging(call: Call) {
                val args = HashMap<String, String>()
                args.put("status", "ringing")
                _channel.invokeMethod("call_listener", args)
            }

            override fun onConnectFailure(call: Call, callException: CallException) {
//                setAudioFocus(false)
                stopWakeLock();
                val message: String = String.format("Call Error:%d, %s", callException.errorCode, callException.message)
                cancelNotification()
                val args = HashMap<String, String>()
                args.put("status", "connect_failure")
                args.put("message", message)
                _channel.invokeMethod("call_listener", args)
            }


            override fun onConnected(call: Call) {
                activeCall = call
                val callSid: String? = call.sid
                val callFrom: String? = call.from
                val args: HashMap<String, String> = HashMap<String, String>()
                args.put("status", "connected")
                if (callSid != null)
                    args.put("sid", callSid)
                if (callFrom != null)
                    args.put("from", callFrom)
                _channel.invokeMethod("call_listener", args)
            }

            override fun onReconnecting(call: Call, callException: CallException) {
            }

            override fun onReconnected(call: Call) {
            }

            override fun onDisconnected(call: Call, callException: CallException?) {
                stopWakeLock()
                val args: HashMap<String, String> = HashMap<String, String>()
                if (callException != null) {
                    val message: String = String.format("Call Error: %d %s", callException.errorCode, callException.message)
                    args.put("message", message)
                }
                cancelNotification()
                args.put("status", "disconnected")
                _channel.invokeMethod("call_listener", args)
            }

        }

    }

    fun stopWakeLock() {
        if (_wakeLock.isHeld)
            _wakeLock.release()
    }

    fun startWakeLock() {
        if (!_wakeLock.isHeld) {
            _wakeLock.acquire(600000L) /* 10* 60* 1000L => 10 Minutes*/
        }
    }

    fun invokeCall(accessToken: String, to: String, locationId: String, callerId: String) {
        params.put("number", to)
        params.put("callerId", callerId)
        params.put("location", locationId)
        val codecList: ArrayList<AudioCodec> = ArrayList<AudioCodec>()
        codecList.add(OpusCodec(8000));
        codecList.add(PcmuCodec())
        val connectOptions: ConnectOptions = ConnectOptions.Builder(accessToken)
                .params(params)
                .preferAudioCodecs(codecList)
                .build()
        activeCall = Voice.connect(_context, connectOptions, callListener)

    }

    fun disconnect() {
        if (activeCall != null) {
            activeCall!!.disconnect()
            activeCall = null
        }
    }

    fun hold(): Boolean {
        if (activeCall != null) {
            val hold: Boolean = !activeCall!!.isOnHold
            activeCall!!.hold(hold)

            return activeCall!!.isOnHold
        }
        return false
    }

    fun mute(): Boolean {
        if (activeCall != null) {
            val mute: Boolean = !activeCall!!.isMuted
            activeCall!!.mute(mute)
            return activeCall!!.isMuted
        }
        return false
    }

    fun speaker(speaker: Boolean): Boolean {
        Log.e(TAG, "speaker value:$speaker")
        try {
            setBluetoothStatus(false)
            _audioManager.setSpeakerphoneOn(speaker)
        } catch (e: Exception) {
        }
        return _audioManager.isSpeakerPhoneOn
    }

    fun keyPress(digit: String) {
        if (activeCall != null) {
            activeCall!!.sendDigits(digit)
        }
    }

    fun setBluetoothStatus(status: Boolean) {
        Log.e(TAG, "bluetooth value:$status")
        _audioManager.setBluetoothPhoneOn(status)
    }

    fun getBluetoothName(): String {
        return _audioManager.bluetoothName
    }
}

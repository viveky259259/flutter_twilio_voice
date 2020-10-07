package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.highlevel.flutter_twilio_voice.flutter_twilio_voice.AppRtc.AppRTCAudioManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


public class FlutterTwilioVoicePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var _field = 0x00000020
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    private lateinit var twilioManager: TwilioManager
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var context: Context
    private lateinit var audioManager: AppRTCAudioManager
    private lateinit var activity: Activity
    var appPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_twilio_voice")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }


    companion object {
        private lateinit var instance: FlutterTwilioVoicePlugin

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            Log.e("Register with", "initiated")
            instance = FlutterTwilioVoicePlugin()
            instance.channel = MethodChannel(registrar.messenger(), "flutter_twilio_voice")
            instance.channel.setMethodCallHandler(instance)
            instance.initPlugin(registrar.activity())
            instance.context = registrar.activeContext()

        }
    }

    fun checkAndRequestPermission(activity: Activity) {
        val listPermissionNeeded: MutableList<String> = ArrayList()
        for (perm in appPermission) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(perm)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    listPermissionNeeded.toTypedArray(),
                    PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "call" -> {
                val isValid: Boolean = isValidDrawableResource(context, call.argument<String>("icon") as String)
                if (isValid)
                    twilioManager.defaultIcon = call.argument<String>("icon") as String
                twilioManager.startCall(call.argument<String>("name") as String,
                        call.argument<String>("accessToken") as String,
                        call.argument<String>("to") as String,
                        call.argument<String>("locationId") as String,
                        call.argument<String>("callerId") as String)
                result.success(true)

            }
            "hold" -> {
                result.success(twilioManager.toggleHold())
            }
            "speaker" -> {
                result.success(twilioManager.toggleSpeaker(call.argument<Boolean>("speaker") as Boolean))
            }
            "mute" -> {
                result.success(twilioManager.toggleMute())
            }
            "keyPress" -> {
                twilioManager.keyPress(call.argument<String>("digit") as String)
                result.success(true)
            }
            "disconnect" -> {
                twilioManager.disconnectCall()
                result.success(true)
            }
            "bluetooth" -> {
                val isBluetooth: Boolean = call.argument<Boolean>("bluetooth") as Boolean
                result.success(twilioManager.setBluetooth(isBluetooth))
            }
            else -> {
                result.success(false)
            }
        }
    }

    private fun isValidDrawableResource(context: Context, name: String): Boolean {
        val resourceId: Int = context.resources.getIdentifier(name, "drawable", context.packageName)
        return resourceId != 0
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    fun initPlugin(activity: Activity) {
        checkAndRequestPermission(activity)
        this.activity = activity
        try {
            _field = PowerManager::class.java.javaClass.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null)
        } catch (e: Exception) {
        }
        // These flags ensure that the activity can be launched when the screen is locked.

        val window: Window = activity.window
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        } else
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        /*
         * Needed for setting/abandoning audio focus during a call
         */
        audioManager = AppRTCAudioManager.create(activity.applicationContext)
        twilioManager = TwilioManager(context = activity,
                activity = activity,
                audioManager = audioManager,
                wakeLock = (activity.getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(_field, activity.localClassName),
                notificationManager = (activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager),
                channel = channel
        )
        activity.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.e("onAttachedToActivity", "initiated")
        initPlugin(binding.activity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
        audioManager.stop()
    }


}

package com.example.myrecordcollection.input

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SteeringWheelController(
    context: Context,
    private val onMediaControllerChanged: (MediaController?) -> Unit = {},
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val consumedKeys = mutableSetOf<Int>()
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _mappings = MutableStateFlow(loadMappings())
    val mappings: StateFlow<Map<SteeringCommand, Int>> = _mappings.asStateFlow()

    private val _pendingCommand = MutableStateFlow<SteeringCommand?>(null)
    val pendingCommand: StateFlow<SteeringCommand?> = _pendingCommand.asStateFlow()

    private val _commands = MutableSharedFlow<SteeringCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<SteeringCommand> = _commands.asSharedFlow()

    private var screenEnabled = false
    private var activityResumed = false
    private var lastKeyCode = KeyEvent.KEYCODE_UNKNOWN
    private var lastEventTime = 0L
    private var pendingClick: PendingClick? = null

    private data class PendingClick(val keyCode: Int, val command: SteeringCommand, val runnable: Runnable)

    @Suppress("DEPRECATION")
    private val mediaSession = MediaSession(context, "MyRecordCollectionSteering").apply {
        setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
        // Some head units route volume through the session instead of Activity key events.
        setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
            override fun onAdjustVolume(direction: Int) {
                mainHandler.post {
                    if (!isActive) return@post
                    val keyCode = when (direction) {
                        AudioManager.ADJUST_RAISE -> KeyEvent.KEYCODE_VOLUME_UP
                        AudioManager.ADJUST_LOWER -> KeyEvent.KEYCODE_VOLUME_DOWN
                        else -> return@post
                    }
                    if (!handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
                    } else {
                        handleKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                    }
                }
            }
        })
        setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                )
                .setState(PlaybackState.STATE_PAUSED, 0L, 1f)
                .build(),
        )
        setCallback(
            object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val event = mediaButtonIntent.keyEvent() ?: return false
                    return handleKeyEvent(event)
                }

                override fun onSkipToNext() {
                    dispatchDefaultMediaCommand(SteeringCommand.NextAlbum)
                }

                override fun onSkipToPrevious() {
                    dispatchDefaultMediaCommand(SteeringCommand.PreviousAlbum)
                }

                override fun onPlay() {
                    dispatchDefaultMediaCommand(SteeringCommand.PlayAlbum)
                }
            },
            mainHandler,
        )
    }

    fun setEnabled(value: Boolean) {
        screenEnabled = value
        updateActiveState()
        if (!value) cancelLearning()
    }

    fun setActivityResumed(value: Boolean) {
        activityResumed = value
        updateActiveState()
    }

    fun startLearning(command: SteeringCommand) {
        _pendingCommand.value = command
    }

    fun cancelLearning() {
        _pendingCommand.value = null
    }

    fun clearMapping(command: SteeringCommand) {
        saveMappings(_mappings.value - command)
    }

    fun clearAllMappings() {
        saveMappings(emptyMap())
    }

    fun resetDefaults() {
        saveMappings(DEFAULT_MAPPINGS)
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!isActive) return false
        if (event.action == KeyEvent.ACTION_UP) return consumedKeys.remove(event.keyCode)
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return event.keyCode in consumedKeys || isMappedOrLearning(event.keyCode)

        _pendingCommand.value?.let { command ->
            val updated = _mappings.value
                .toMutableMap()
                .apply { put(command, event.keyCode) }
            saveMappings(updated)
            _pendingCommand.value = null
            consumedKeys.add(event.keyCode)
            return true
        }

        val command = _mappings.value.entries
            .firstOrNull { (_, keyCode) -> keyCode == event.keyCode }
            ?.key
            ?: return false
        val previous = pendingClick
        if (previous != null && previous.keyCode == event.keyCode) {
            mainHandler.removeCallbacks(previous.runnable)
            pendingClick = null
            val doubleCommand = doubleCommand(command)
            if (doubleCommand != null && _mappings.value[doubleCommand] == event.keyCode) {
                emitDebounced(doubleCommand, event.keyCode)
            } else {
                emitDebounced(previous.command, event.keyCode)
                emitDebounced(command, event.keyCode)
            }
        } else {
            pendingClick?.let { mainHandler.removeCallbacks(it.runnable) }
            val runnable = Runnable {
                if (pendingClick?.keyCode == event.keyCode) {
                    pendingClick = null
                    emitDebounced(command, event.keyCode)
                }
            }
            pendingClick = PendingClick(event.keyCode, command, runnable)
            mainHandler.postDelayed(runnable, DOUBLE_CLICK_TIMEOUT_MILLIS)
        }
        consumedKeys.add(event.keyCode)
        return true
    }

    fun release() {
        onMediaControllerChanged(null)
        mediaSession.release()
    }

    private fun dispatchDefaultMediaCommand(command: SteeringCommand) {
        if (isActive && _pendingCommand.value == null) {
            val keyCode = _mappings.value[command] ?: command.ordinal
            handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        }
    }

    private fun emitDebounced(command: SteeringCommand, keyCode: Int) {
        val now = SystemClock.elapsedRealtime()
        if (keyCode == lastKeyCode && now - lastEventTime < DEBOUNCE_MILLIS) return
        lastKeyCode = keyCode
        lastEventTime = now
        _commands.tryEmit(command)
    }

    private fun isMappedOrLearning(keyCode: Int): Boolean =
        _pendingCommand.value != null || keyCode in _mappings.value.values

    private fun loadMappings(): Map<SteeringCommand, Int> {
        if (!preferences.getBoolean(CUSTOMIZED_KEY, false)) return DEFAULT_MAPPINGS
        return SteeringCommand.entries
        .mapNotNull { command ->
            val preferenceKey = command.preferenceKey
            if (preferences.contains(preferenceKey)) {
                command to preferences.getInt(preferenceKey, KeyEvent.KEYCODE_UNKNOWN)
            } else null
        }
        .filter { (_, keyCode) -> keyCode != KeyEvent.KEYCODE_UNKNOWN }
        .toMap()
    }

    private fun updateActiveState() {
        mediaSession.isActive = isActive
        onMediaControllerChanged(if (isActive) mediaSession.controller else null)
        if (!isActive) consumedKeys.clear()
    }

    private val isActive: Boolean
        get() = screenEnabled && activityResumed

    private fun saveMappings(mappings: Map<SteeringCommand, Int>) {
        preferences.edit().apply {
            SteeringCommand.entries.forEach { remove(it.preferenceKey) }
            mappings.forEach { (command, keyCode) -> putInt(command.preferenceKey, keyCode) }
            putBoolean(CUSTOMIZED_KEY, true)
        }.apply()
        _mappings.value = mappings
    }

    private fun doubleCommand(command: SteeringCommand): SteeringCommand? = when (command) {
        SteeringCommand.NextAlbum -> SteeringCommand.DoubleNextAlbum
        SteeringCommand.PreviousAlbum -> SteeringCommand.DoublePreviousAlbum
        SteeringCommand.NextArtist -> SteeringCommand.DoubleNextArtist
        SteeringCommand.PreviousArtist -> SteeringCommand.DoublePreviousArtist
        SteeringCommand.PlayAlbum -> SteeringCommand.DoublePlayAlbum
        else -> null
    }

    @Suppress("DEPRECATION")
    private fun Intent.keyEvent(): KeyEvent? =
        getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent

    private val SteeringCommand.preferenceKey: String
        get() = "command_$name"

    private companion object {
        const val PREFERENCES_NAME = "steering_wheel_controls"
        const val CUSTOMIZED_KEY = "customized"
        const val DEBOUNCE_MILLIS = 180L
        const val DOUBLE_CLICK_TIMEOUT_MILLIS = 280L

        val DEFAULT_MAPPINGS = mapOf(
            SteeringCommand.NextAlbum to KeyEvent.KEYCODE_MEDIA_NEXT,
            SteeringCommand.PreviousAlbum to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            SteeringCommand.NextArtist to KeyEvent.KEYCODE_VOLUME_UP,
            SteeringCommand.PreviousArtist to KeyEvent.KEYCODE_VOLUME_DOWN,
            SteeringCommand.PlayAlbum to KeyEvent.KEYCODE_CALL,
            SteeringCommand.DoubleNextAlbum to KeyEvent.KEYCODE_MEDIA_NEXT,
            SteeringCommand.DoublePreviousAlbum to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            SteeringCommand.DoubleNextArtist to KeyEvent.KEYCODE_VOLUME_UP,
            SteeringCommand.DoublePreviousArtist to KeyEvent.KEYCODE_VOLUME_DOWN,
            SteeringCommand.DoublePlayAlbum to KeyEvent.KEYCODE_CALL,
        )
    }
}

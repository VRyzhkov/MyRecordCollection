package com.example.myrecordcollection.input

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SteeringWheelController(context: Context) {
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

    @Suppress("DEPRECATION")
    private val mediaSession = MediaSession(context, "MyRecordCollectionSteering").apply {
        setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
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
        if (!isActive || event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return isMappedOrLearning(event.keyCode)

        _pendingCommand.value?.let { command ->
            val updated = _mappings.value
                .filterValues { it != event.keyCode }
                .toMutableMap()
                .apply { put(command, event.keyCode) }
            saveMappings(updated)
            _pendingCommand.value = null
            return true
        }

        val command = _mappings.value.entries
            .firstOrNull { (_, keyCode) -> keyCode == event.keyCode }
            ?.key
            ?: return false
        emitDebounced(command, event.keyCode)
        return true
    }

    fun release() {
        mediaSession.release()
    }

    private fun dispatchDefaultMediaCommand(command: SteeringCommand) {
        if (isActive && _pendingCommand.value == null) emitDebounced(command, command.ordinal)
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

    @Suppress("DEPRECATION")
    private fun Intent.keyEvent(): KeyEvent? =
        getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent

    private val SteeringCommand.preferenceKey: String
        get() = "command_$name"

    private companion object {
        const val PREFERENCES_NAME = "steering_wheel_controls"
        const val CUSTOMIZED_KEY = "customized"
        const val DEBOUNCE_MILLIS = 180L

        val DEFAULT_MAPPINGS = mapOf(
            SteeringCommand.NextAlbum to KeyEvent.KEYCODE_MEDIA_NEXT,
            SteeringCommand.PreviousAlbum to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            SteeringCommand.NextArtist to KeyEvent.KEYCODE_VOLUME_UP,
            SteeringCommand.PreviousArtist to KeyEvent.KEYCODE_VOLUME_DOWN,
            SteeringCommand.PlayAlbum to KeyEvent.KEYCODE_CALL,
        )
    }
}

package com.example.timedsilence

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.timedsilence.worker.RingerRestorationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val sharedPrefs = application.getSharedPreferences("timed_silence_prefs", Context.MODE_PRIVATE)

    private var internalWorkManager: WorkManager? = null
    
    private val workManager: WorkManager
        get() = internalWorkManager ?: WorkManager.getInstance(getApplication())

    fun setWorkManager(wm: WorkManager) {
        internalWorkManager = wm
        observeWorkStatus()
    }

    private val _isSilenced = MutableStateFlow(false)
    val isSilenced: StateFlow<Boolean> = _isSilenced.asStateFlow()

    companion object {
        const val KEY_CAPTURED_MODE = "captured_mode"
        const val KEY_CAPTURED_VOLUME = "captured_volume"
        const val KEY_END_TIME = "end_time_millis"
        private const val CHANNEL_ID = "timed_silence_channel"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
        observeWorkStatus()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timed Silence Status"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                description = "Displays active silence periods"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun observeWorkStatus() {
        viewModelScope.launch {
            try {
                workManager.getWorkInfosForUniqueWorkLiveData("restoration_work")
                    .asFlow()
                    .collect { workInfos ->
                        val isActive = workInfos.any { 
                            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING 
                        }
                        _isSilenced.value = isActive
                        
                        if (!isActive) {
                            clearCapturedState()
                            notificationManager.cancel(NOTIFICATION_ID)
                        }
                    }
            } catch (e: Exception) {
                // Log exception
            }
        }
    }

    fun startSilence(durationMinutes: Int, targetMode: Int) {
        if (_isSilenced.value) return
        
        try {
            val modeToRestore = audioManager.ringerMode
            val volumeToRestore = try {
                audioManager.getStreamVolume(AudioManager.STREAM_RING)
            } catch (e: Exception) {
                0
            }
            
            // Round end time to the closest minute
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, durationMinutes)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val endTimeMillis = calendar.timeInMillis
            
            saveCapturedState(modeToRestore, volumeToRestore, endTimeMillis)

            audioManager.ringerMode = targetMode
            
            val inputData = Data.Builder()
                .putInt(RingerRestorationWorker.KEY_ORIGINAL_MODE, modeToRestore)
                .putInt(RingerRestorationWorker.KEY_ORIGINAL_VOLUME, volumeToRestore)
                .build()

            val restorationWork = OneTimeWorkRequestBuilder<RingerRestorationWorker>()
                .setInitialDelay(durationMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("ringer_restoration")
                .build()

            workManager.enqueueUniqueWork(
                "restoration_work",
                ExistingWorkPolicy.REPLACE,
                restorationWork
            )

            showOngoingNotification(endTimeMillis)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error starting silence", e)
        }
    }

    private fun showOngoingNotification(endTimeMillis: Long) {
        val endTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTimeMillis)

        val notification = NotificationCompat.Builder(getApplication(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("Timed Silence Active")
            .setContentText("Active until $endTimeStr")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelSilence() {
        workManager.cancelUniqueWork("restoration_work")
        
        val mode = sharedPrefs.getInt(KEY_CAPTURED_MODE, -1)
        val volume = sharedPrefs.getInt(KEY_CAPTURED_VOLUME, -1)

        if (mode != -1) {
            audioManager.ringerMode = mode
            if (volume != -1) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        
        clearCapturedState()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun saveCapturedState(mode: Int, volume: Int, endTimeMillis: Long) {
        sharedPrefs.edit {
            putInt(KEY_CAPTURED_MODE, mode)
            putInt(KEY_CAPTURED_VOLUME, volume)
            putLong(KEY_END_TIME, endTimeMillis)
        }
    }

    private fun clearCapturedState() {
        sharedPrefs.edit { clear() }
    }
}

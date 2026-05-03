package com.example.timedsilence.worker

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RingerRestorationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val originalMode = inputData.getInt(KEY_ORIGINAL_MODE, AudioManager.RINGER_MODE_NORMAL)
        val originalVolume = inputData.getInt(KEY_ORIGINAL_VOLUME, -1)

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        return try {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                Log.d("RingerRestorationWorker", "Restoring ringer mode to $originalMode")
                audioManager.ringerMode = originalMode
                
                if (originalVolume != -1) {
                    Log.d("RingerRestorationWorker", "Restoring ringer volume to $originalVolume")
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_RING,
                        originalVolume,
                        0
                    )
                }
                androidx.work.ListenableWorker.Result.success()
            } else {
                Log.e("RingerRestorationWorker", "Notification policy access NOT granted")
                androidx.work.ListenableWorker.Result.failure()
            }
        } catch (e: Exception) {
            Log.e("RingerRestorationWorker", "Error restoring ringer mode", e)
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    companion object {
        const val KEY_ORIGINAL_MODE = "original_mode"
        const val KEY_ORIGINAL_VOLUME = "original_volume"
    }
}

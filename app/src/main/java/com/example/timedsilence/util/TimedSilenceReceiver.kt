package com.example.timedsilence.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.timedsilence.MainViewModel
import com.example.timedsilence.worker.RingerRestorationWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.app.NotificationCompat

class TimedSilenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val workManager = WorkManager.getInstance(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sharedPrefs = context.getSharedPreferences("timed_silence_prefs", Context.MODE_PRIVATE)

        when (action) {
            ACTION_STOP -> {
                Log.d("TimedSilenceReceiver", "Stopping silence via notification action")
                workManager.cancelUniqueWork("restoration_work")
                
                val mode = sharedPrefs.getInt("captured_mode", -1)
                val volume = sharedPrefs.getInt("captured_volume", -1)

                if (mode != -1) {
                    audioManager.ringerMode = mode
                    if (volume != -1) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0)
                    }
                }
                
                sharedPrefs.edit { clear() }
                notificationManager.cancel(MainViewModel.NOTIFICATION_ID)
            }
            ACTION_EXTEND -> {
                Log.d("TimedSilenceReceiver", "Extending silence via notification action")
                val modeToRestore = sharedPrefs.getInt("captured_mode", -1)
                val volumeToRestore = sharedPrefs.getInt("captured_volume", -1)
                
                if (modeToRestore == -1) return

                // Add 15 minutes to the extension
                val extensionMinutes = 15
                
                // We need to calculate the new total duration or just reschedule the work.
                // However, we don't easily know the remaining time from WorkManager here.
                // A simpler approach for "Extend" is to just set a new OneTimeWorkRequest 
                // starting from NOW + 15 minutes (or some other logic).
                
                // Let's assume Extend means "Add 15 minutes to the current scheduled time".
                // Since we don't track the end time in prefs yet, let's start tracking it.
                
                val currentEndTime = sharedPrefs.getLong("end_time_millis", System.currentTimeMillis())
                val newEndTime = currentEndTime + TimeUnit.MINUTES.toMillis(extensionMinutes.toLong())
                val delayMillis = newEndTime - System.currentTimeMillis()
                
                sharedPrefs.edit {
                    putLong("end_time_millis", newEndTime)
                }

                val inputData = Data.Builder()
                    .putInt(RingerRestorationWorker.KEY_ORIGINAL_MODE, modeToRestore)
                    .putInt(RingerRestorationWorker.KEY_ORIGINAL_VOLUME, volumeToRestore)
                    .build()

                val restorationWork = OneTimeWorkRequestBuilder<RingerRestorationWorker>()
                    .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("ringer_restoration")
                    .build()

                workManager.enqueueUniqueWork(
                    "restoration_work",
                    ExistingWorkPolicy.REPLACE,
                    restorationWork
                )

                // Update notification
                updateNotification(context, notificationManager, newEndTime)
            }
        }
    }

    private fun updateNotification(context: Context, notificationManager: NotificationManager, endTimeMillis: Long) {
        val endTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTimeMillis)
        
        val stopIntent = Intent(context, TimedSilenceReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 0, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val extendIntent = Intent(context, TimedSilenceReceiver::class.java).apply {
            action = ACTION_EXTEND
        }
        val extendPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 1, extendIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "timed_silence_channel")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("Timed Silence Extended")
            .setContentText("Phone will be restored at $endTimeStr")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Extend +15m", extendPendingIntent)
            .build()

        notificationManager.notify(MainViewModel.NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_STOP = "com.example.timedsilence.ACTION_STOP"
        const val ACTION_EXTEND = "com.example.timedsilence.ACTION_EXTEND"
    }
}

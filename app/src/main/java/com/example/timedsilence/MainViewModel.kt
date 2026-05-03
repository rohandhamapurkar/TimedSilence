package com.example.timedsilence

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.timedsilence.worker.RingerRestorationWorker
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val workManager = WorkManager.getInstance(application)

    fun startSilence(durationMinutes: Int) {
        try {
            // 1. Capture current ringer mode and volume
            val originalMode = audioManager.ringerMode
            val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            
            Log.d("MainViewModel", "Capturing original state: Mode=$originalMode, Volume=$originalVolume")

            // 2. Silence the phone
            // Since we check for DND permission before calling this, we can set to Silent mode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            
            // 3. Schedule restoration
            val inputData = Data.Builder()
                .putInt(RingerRestorationWorker.KEY_ORIGINAL_MODE, originalMode)
                .putInt(RingerRestorationWorker.KEY_ORIGINAL_VOLUME, originalVolume)
                .build()

            val restorationWork = OneTimeWorkRequestBuilder<RingerRestorationWorker>()
                .setInitialDelay(durationMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("ringer_restoration")
                .build()

            // Use unique work to ensure only one restoration is pending
            workManager.enqueueUniqueWork(
                "restoration_work",
                ExistingWorkPolicy.REPLACE,
                restorationWork
            )
            Log.d("MainViewModel", "Restoration scheduled in $durationMinutes minutes (Unique)")

        } catch (e: Exception) {
            Log.e("MainViewModel", "Error during silencing process", e)
        }
    }
}

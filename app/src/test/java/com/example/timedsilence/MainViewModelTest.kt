package com.example.timedsilence

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    lateinit var application: Application
    @Mock
    lateinit var audioManager: AudioManager
    @Mock
    lateinit var notificationManager: NotificationManager
    @Mock
    lateinit var workManager: WorkManager
    @Mock
    lateinit var sharedPreferences: SharedPreferences
    @Mock
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        `when`(application.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        `when`(application.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
        `when`(application.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor)
        `when`(sharedPreferencesEditor.putLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(sharedPreferencesEditor)
        `when`(sharedPreferencesEditor.clear()).thenReturn(sharedPreferencesEditor)
        
        `when`(workManager.getWorkInfosForUniqueWorkLiveData(anyString())).thenReturn(MutableLiveData<List<WorkInfo>>())

        `when`(application.applicationContext).thenReturn(application)
        `when`(application.packageName).thenReturn("com.example.timedsilence")
        `when`(application.resources).thenReturn(mock(android.content.res.Resources::class.java))

        viewModel = MainViewModel(application)
        viewModel.setWorkManager(workManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startSilence captures state and sets mode`() {
        val originalMode = AudioManager.RINGER_MODE_NORMAL
        val originalVolume = 7
        val targetMode = AudioManager.RINGER_MODE_VIBRATE
        val duration = 30

        `when`(audioManager.ringerMode).thenReturn(originalMode)
        `when`(audioManager.getStreamVolume(AudioManager.STREAM_RING)).thenReturn(originalVolume)

        viewModel.startSilence(duration, targetMode)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state capture
        verify(sharedPreferencesEditor).putInt(MainViewModel.KEY_CAPTURED_MODE, originalMode)
        verify(sharedPreferencesEditor).putInt(MainViewModel.KEY_CAPTURED_VOLUME, originalVolume)
        verify(sharedPreferencesEditor).putLong(org.mockito.ArgumentMatchers.eq(MainViewModel.KEY_END_TIME), org.mockito.ArgumentMatchers.anyLong())
        verify(sharedPreferencesEditor, atLeastOnce()).apply()

        // Verify ringer change
        verify(audioManager).ringerMode = targetMode
    }

    @Test
    fun `cancelSilence restores state and clears prefs`() {
        val savedMode = AudioManager.RINGER_MODE_NORMAL
        val savedVolume = 5
        
        `when`(sharedPreferences.getInt(MainViewModel.KEY_CAPTURED_MODE, -1)).thenReturn(savedMode)
        `when`(sharedPreferences.getInt(MainViewModel.KEY_CAPTURED_VOLUME, -1)).thenReturn(savedVolume)

        viewModel.cancelSilence()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify restoration
        verify(audioManager).ringerMode = savedMode
        verify(audioManager).setStreamVolume(AudioManager.STREAM_RING, savedVolume, 0)

        // Verify cleanup
        verify(sharedPreferencesEditor).clear()
        verify(notificationManager).cancel(MainViewModel.NOTIFICATION_ID)
    }
}

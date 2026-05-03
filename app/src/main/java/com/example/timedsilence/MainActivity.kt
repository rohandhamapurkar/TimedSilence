package com.example.timedsilence

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timedsilence.ui.theme.TimedSilenceTheme
import com.example.timedsilence.util.PermissionUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimedSilenceTheme {
                TimedSilenceApp(viewModel)
            }
        }
    }
}

@Composable
fun TimedSilenceApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isSilenced by viewModel.isSilenced.collectAsState()
    var selectedDuration by remember { mutableStateOf(30) }
    
    // Requirement 3: Vibrate as default
    var selectedMode by remember { mutableStateOf(AudioManager.RINGER_MODE_VIBRATE) }
    
    // Requirement 5: Permission handling
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Permission denied for notifications.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSilenced) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsPaused,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = if (isSilenced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSilenced) "Silence Active" else "Timed Silence",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
            )

            Text(
                text = if (isSilenced) "Phone handled automatically." else "Set a duration and mode.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isSilenced) {
                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == AudioManager.RINGER_MODE_SILENT,
                        onClick = { selectedMode = AudioManager.RINGER_MODE_SILENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Silent") }
                    )
                    SegmentedButton(
                        selected = selectedMode == AudioManager.RINGER_MODE_VIBRATE,
                        onClick = { selectedMode = AudioManager.RINGER_MODE_VIBRATE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Vibrate") }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Duration (Minutes)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                
                MinutePicker(
                    selectedMinutes = selectedDuration,
                    onMinutesSelected = { selectedDuration = it }
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        handleStartSilence(context, viewModel, selectedDuration, selectedMode)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Start Silence", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Text(
                    text = "Active session in progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedButton(
                    onClick = { viewModel.cancelSilence() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Cancel & Restore", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
fun TimedSilenceAppPreview() {
    TimedSilenceTheme {
        TimedSilenceApp(viewModel = MainViewModel(LocalContext.current.applicationContext as android.app.Application))
    }
}

@Composable
fun MinutePicker(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit
) {
    val minutesRange = remember { (1..120).toList() }
    val itemHeight = 60.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMinutes - 1)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Requirement 2: High-performance mathematical scroll calculation (no list searches)
    LaunchedEffect(listState) {
        snapshotFlow { 
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset
            
            // At scroll 0, item 0 is centered due to contentPadding.
            // Each itemHeightPx of scroll moves the next item into the center.
            val centeredIndex = firstVisibleIndex + Math.round(firstVisibleOffset / itemHeightPx).toInt()
            
            centeredIndex.coerceIn(0, minutesRange.size - 1)
        }
        .distinctUntilChanged()
        .collect { index ->
            onMinutesSelected(minutesRange[index])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent, 0.3f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 16.dp)
                .graphicsLayer { alpha = 0.1f }
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(minutesRange) { index, minute ->
                val isSelected = selectedMinutes == minute
                
                // Requirement 2: Performant distance calculation for scaling
                val scale by remember(index) {
                    derivedStateOf {
                        val firstVisibleIndex = listState.firstVisibleItemIndex
                        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                        
                        // Distance calculation relative to the "centered" position
                        val itemOffset = (index - firstVisibleIndex) * itemHeightPx - firstVisibleOffset
                        val distanceFromCenter = abs(itemOffset)
                        
                        (1f - (distanceFromCenter / 300f)).coerceIn(0.7f, 1f)
                    }
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale; alpha = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = minute.toString(),
                        style = if (isSelected) 
                            MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold) 
                        else 
                            MaterialTheme.typography.headlineSmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun handleStartSilence(context: Context, viewModel: MainViewModel, duration: Int, mode: Int) {
    if (PermissionUtils.hasNotificationPolicyAccess(context)) {
        viewModel.startSilence(duration, mode)
    } else {
        PermissionUtils.requestNotificationPolicyAccess(context)
    }
}

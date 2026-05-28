package com.raju.edutrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.raju.edutrack.update.UpdateCheckResult
import com.raju.edutrack.update.UpdateManager
import kotlinx.coroutines.launch
import com.raju.edutrack.ui.theme.EduTrackTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        StudentManager.load(this)
        AppSettings.load(this)
        BatchManager.load(this)

        lifecycleScope.launch {
            when (val result = UpdateManager.checkForUpdate(this@MainActivity)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    UpdateManager.installUpdate(this@MainActivity, result.release.downloadUrl)
                }
                else -> Unit
            }
        }

        enableEdgeToEdge()

        setContent {

            EduTrackTheme {

                AppNavigation()

            }
        }
    }
}

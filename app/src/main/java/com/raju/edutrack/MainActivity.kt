package com.raju.edutrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raju.edutrack.cloud.CloudBackupManager
import com.raju.edutrack.ui.theme.EduTrackTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        StudentManager.load(this)
        AppSettings.load(this)
        BatchManager.load(this)
        CloudBackupManager.load(this)

        enableEdgeToEdge()

        setContent {

            EduTrackTheme {

                AppNavigation()

            }
        }
    }
}

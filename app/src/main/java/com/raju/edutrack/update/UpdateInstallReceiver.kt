package com.raju.edutrack.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val message = intent.getStringExtra(
            PackageInstaller.EXTRA_STATUS_MESSAGE
        ) ?: ""

        if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(
                context,
                "Update installed",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Update failed: $message",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

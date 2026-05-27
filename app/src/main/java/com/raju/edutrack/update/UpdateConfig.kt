package com.raju.edutrack.update

object UpdateConfig {
    const val owner = "YOUR_GITHUB_OWNER"
    const val repo = "YOUR_REPO"
    const val assetNameContains = ".apk"

    val isConfigured: Boolean
        get() = owner != "YOUR_GITHUB_OWNER" && repo != "YOUR_REPO"
}

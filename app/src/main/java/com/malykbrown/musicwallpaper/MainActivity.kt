package com.malykbrown.musicwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("mw_prefs", MODE_PRIVATE)

        findViewById<Button>(R.id.btnNotif).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnSetWallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, MusicWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }

        val blurSeek = findViewById<SeekBar>(R.id.seekBlur)
        blurSeek.progress = prefs.getInt("blur", 0)
        blurSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                prefs.edit().putInt("blur", value).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val modeGroup = findViewById<RadioGroup>(R.id.modeGroup)
        if (prefs.getString("mode", "full") == "full")
            modeGroup.check(R.id.modeFull) else modeGroup.check(R.id.modeCenter)
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putString(
                "mode",
                if (checkedId == R.id.modeFull) "full" else "center"
            ).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        watchJob = scope.launch {
            NowPlaying.state.collect { updateStatus() }
        }
    }

    override fun onPause() {
        watchJob?.cancel()
        super.onPause()
    }

    private fun updateStatus() {
        val granted = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
        val info = NowPlaying.state.value
        val status = findViewById<TextView>(R.id.txtStatus)
        status.text = when {
            !granted -> "1️⃣ Falta o acesso às notificações"
            info.title.isEmpty() -> "✓ Pronto. Toca uma música para testar!"
            else -> "🎵 ${info.title} — ${info.artist}"
        }
    }
}

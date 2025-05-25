package com.example.tiktokparser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartTikTok = findViewById<Button>(R.id.btnStartTikTok)
        btnStartTikTok.setOnClickListener {
            val pm = packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.zhiliaoapp.musically")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("market://details?id=com.zhiliaoapp.musically")
                startActivity(intent)
            }
        }

        val btnAccessibilitySettings = findViewById<Button>(R.id.btnAccessibility)
        btnAccessibilitySettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }
}

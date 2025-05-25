package com.example.tiktokparser

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("TikTokParser", "YoutubeDL и FFmpeg успешно инициализированы")
        } catch (e: Exception) {
            Log.e("TikTokParser", "Ошибка инициализации: ${e.message}")
        }
    }
}

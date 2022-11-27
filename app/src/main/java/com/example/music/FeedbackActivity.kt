package com.example.music

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Music)
        setContentView(R.layout.activity_feedback)
    }
}
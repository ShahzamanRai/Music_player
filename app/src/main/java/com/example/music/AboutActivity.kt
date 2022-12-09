package com.example.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.music.databinding.ActivityAboutBinding


class AboutActivity : AppCompatActivity() {
    lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_Music)
        setContentView(binding.root)
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.linkedin.setOnClickListener {
            openLinkPage("https://www.linkedin.com/in/shah-zaman-510534243/")
        }
        binding.github.setOnClickListener {
            openLinkPage("https://github.com/ShahzamanRai")
        }
        binding.instagram.setOnClickListener {
            openLinkPage("https://www.instagram.com/shahzaman_rai/")
        }

    }

    private fun openLinkPage(id: String) {
        var intent = Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://add/%@$id"))
        intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(id)
        )
        startActivity(intent)
    }
}
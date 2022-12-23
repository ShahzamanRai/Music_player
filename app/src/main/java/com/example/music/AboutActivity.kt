package com.example.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
        binding.gmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("srshahzaman444@gmail.com"))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }

    }

    private fun openLinkPage(id: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(id)
        )
        startActivity(intent)
    }
}
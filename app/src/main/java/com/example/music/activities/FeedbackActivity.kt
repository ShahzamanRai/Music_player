package com.example.music.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.music.R
import com.example.music.databinding.ActivityFeedbackBinding

class FeedbackActivity : AppCompatActivity() {
    lateinit var binding: ActivityFeedbackBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_Music)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        binding.submitButton.setOnClickListener {
            val isNotEmpty =
                binding.nameFeedback.text?.isNotEmpty() == true && binding.messageFeedback.text?.isNotEmpty() == true
            if (isNotEmpty) {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:") // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("srshahzaman444@gmail.com"))
                    intent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Feedback from " + binding.nameFeedback.text.toString()
                    )
                    intent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Dear Shahzaman, " + binding.messageFeedback.text.toString()
                    )
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                    Toast.makeText(this, "Thanks for Feedback", Toast.LENGTH_SHORT).show()
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Fields can't be empty", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
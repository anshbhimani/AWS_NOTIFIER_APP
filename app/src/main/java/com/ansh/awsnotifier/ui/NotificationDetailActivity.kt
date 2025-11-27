package com.ansh.awsnotifier.ui

import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.ansh.awsnotifier.R
import com.ansh.awsnotifier.databinding.ActivityNotifDetailBinding
import kotlin.math.max

class NotificationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotifDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotifDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ------------------------------
        // Load Intent Data
        // ------------------------------
        val title = intent.getStringExtra("title") ?: getString(R.string.notif_topic)
        val message = intent.getStringExtra("message")
        val topic = intent.getStringExtra("topic") ?: "Unknown Topic"
        val time = intent.getLongExtra("time", 0)

        binding.title.text = title
        binding.message.text = message

        binding.topic.text = getString(R.string.notif_topic, topic)

        binding.time.text = android.text.format.DateFormat.format(
            "EEE, dd MMM yyyy • hh:mm a",
            time
        )

        // ------------------------------
        // 🎨 One UI 8 Visual Enhancements
        // ------------------------------

        applyEntryAnimations()
        applyChipAnimation()
        enableScrollFadeEffect()
    }

    // Smooth fade, slide animation for card (One UI)
    private fun applyEntryAnimations() {
        binding.messageCard.alpha = 0f
        binding.messageCard.translationY = 20f

        binding.messageCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // Chip pop animation (One UI subtle motion)
    private fun applyChipAnimation() {
        binding.topic.scaleX = 0.8f
        binding.topic.scaleY = 0.8f

        binding.topic.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // Title fades out as you scroll down (Samsung UX)
    private fun enableScrollFadeEffect() {
        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            val offset = binding.scrollView.scrollY
            val fade = max(0f, 1f - (offset / 200f))
            binding.title.alpha = fade
        }
    }
}

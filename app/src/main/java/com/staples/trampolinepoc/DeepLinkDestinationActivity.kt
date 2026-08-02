package com.staples.trampolinepoc

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

/**
 * Stand-in for a real native destination (e.g. PDP).
 */
class DeepLinkDestinationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(EXTRA_PATH) ?: "(no path)"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 128, 48, 48)
            setBackgroundColor(ContextCompat.getColor(this@DeepLinkDestinationActivity, R.color.background))
        }

        if (path == "/product/ergonomic-chair") {
            layout.addView(
                ImageView(this).apply {
                    setImageResource(R.drawable.ic_chair)
                    layoutParams = LinearLayout.LayoutParams(256, 256).apply {
                        bottomMargin = 48
                    }
                    setColorFilter(ContextCompat.getColor(this@DeepLinkDestinationActivity, R.color.primary))
                },
            )
        }
        
        setContentView(layout)
    }

    companion object {
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_CAPTION = "extra_caption"
    }
}

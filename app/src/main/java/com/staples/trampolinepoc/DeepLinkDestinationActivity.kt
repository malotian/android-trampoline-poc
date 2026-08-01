package com.staples.trampolinepoc

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

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
        }

        layout.addView(TextView(this).apply {
            text = getString(R.string.dest_reached_success, path)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
        })
        
        setContentView(layout)
    }

    companion object {
        const val EXTRA_PATH = "extra_path"
    }
}

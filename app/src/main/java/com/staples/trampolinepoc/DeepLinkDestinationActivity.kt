package com.staples.trampolinepoc

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Stand-in for a real product/category screen. In the actual app this would
 * be your existing PDP/PLP/etc. — this POC just proves the Trampoline routed
 * the App Deep Link bucket correctly by displaying the path it received.
 */
class DeepLinkDestinationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(EXTRA_PATH) ?: "(no path)"

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(48, 128, 48, 48)
        }

        if (path.contains("chair", ignoreCase = true)) {
            layout.addView(android.widget.TextView(this).apply {
                text = "\uD83E\uDE91" // Chair emoji
                textSize = 100f
                gravity = android.view.Gravity.CENTER
            })
        }

        val textView = android.widget.TextView(this).apply {
            text = getString(R.string.dest_reached_success, path)
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 48, 0, 0)
        }
        layout.addView(textView)
        
        setContentView(layout)
    }

    companion object {
        const val EXTRA_PATH = "extra_path"
    }
}

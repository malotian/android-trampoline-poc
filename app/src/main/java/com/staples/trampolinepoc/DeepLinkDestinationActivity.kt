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

        val textView = TextView(this).apply {
            text = getString(R.string.dest_reached_success, path)
            textSize = 18f
            setPadding(48, 96, 48, 48)
        }
        setContentView(textView)
    }

    companion object {
        const val EXTRA_PATH = "extra_path"
    }
}

package com.staples.trampolinepoc

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Test harness for the Trampoline pattern.
 * Each button fires a real intent through TrampolineActivity so routing logic is exercised end-to-end.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 128, 64, 64)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.background))
        }

        // Android Version Info
        layout.addView(
            TextView(this).apply {
                text = getString(R.string.android_version_label, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 80)
            },
        )

        val domain = "www.staples.com"
        layout.addView(trampolineButton("🛒  Product Page  →  Native Screen",        "https://$domain/product/ergonomic-chair"))
        layout.addView(trampolineButton("🔐  Auth / Login  →  Custom Tab Overlay",   "https://$domain/idm/api/identityProxy/sdc/login"))
        layout.addView(trampolineButton("📋  Promo / Legal  →  System Browser",       "https://$domain/lp/easyrewardsoverview"))

        setContentView(layout)
    }

    private fun trampolineButton(label: String, url: String) = Button(this, null, 0, R.style.ButtonPrimary).apply {
        text = label
        
        // Add vertical spacing between buttons
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = 48
        }

        setOnClickListener {
            Log.d(TAG, "Firing test intent: $url")
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                setClass(this@MainActivity, TrampolineActivity::class.java)
            })
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

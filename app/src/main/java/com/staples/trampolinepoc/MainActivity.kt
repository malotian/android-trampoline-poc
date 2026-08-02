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
import android.widget.Toast
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
        layout.addView(trampolineButton("Product Page → Native Screen", "https://$domain/product/ergonomic-chair", R.drawable.ic_chair))
        layout.addView(trampolineButton("Auth / Login → Custom Tab Overlay", "https://$domain/idm/api/identityProxy/sdc/login", R.drawable.ic_lock))
        layout.addView(trampolineButton("Promo / Legal → System Browser", "https://$domain/lp/easyrewardsoverview", R.drawable.ic_legal))

        setContentView(layout)
    }

    private fun trampolineButton(label: String, url: String, iconRes: Int? = null) = Button(this, null, 0, R.style.ButtonPrimary).apply {
        text = label
        iconRes?.let {
            setCompoundDrawablesWithIntrinsicBounds(it, 0, 0, 0)
            compoundDrawablePadding = 32
        }
        
        // Add vertical spacing between buttons
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = 48
        }

        setOnClickListener {
            Log.d(TAG, "Firing test intent: $url")
            Toast.makeText(this@MainActivity, "Action: $url", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                setClass(this@MainActivity, TrampolineActivity::class.java)
                putExtra(TrampolineActivity.EXTRA_CAPTION, label)
            })
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

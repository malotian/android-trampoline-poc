package com.staples.trampolinepoc

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Simplified manual test harness for the Trampoline pattern.
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
        layout.addView(TextView(this).apply {
            text = getString(R.string.android_version_label, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        })

        val domain = "www.staples.com"
        
        // 1. App Deep Link Bucket
        layout.addView(testButton("App-Deep-Link (Ergonomic Chair)", "http://$domain/product/ergonomic-chair"))
        addSpacer(layout)
        
        // 2. Auth Bucket (Overlay)
        layout.addView(testButton("App-Overlay-Browser (SDC Login)", "http://$domain/idm/api/identityProxy/sdc/login"))
        addSpacer(layout)
        
        // 3. Browser-Only Bucket (System)
        layout.addView(testButton("System-Browser (Easy Rewards)", "http://$domain/lp/easyrewardsoverview"))

        setContentView(layout)
    }

    private fun addSpacer(layout: LinearLayout) {
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, 48)
        })
    }

    private fun testButton(label: String, url: String) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(android.graphics.Color.WHITE)
        textSize = 16f
        
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(ContextCompat.getColor(context, R.color.primary))
        }
        setPadding(32, 40, 32, 40)
        
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

        setOnClickListener {
            Log.d("MainActivity", "Firing explicit test intent: $url")
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                setClass(this@MainActivity, TrampolineActivity::class.java)
            })
        }
    }
}

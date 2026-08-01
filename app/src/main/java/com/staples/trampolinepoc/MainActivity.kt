package com.staples.trampolinepoc

import android.app.Activity
import android.content.Intent
import android.graphics.Color
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
 * Manual test harness for the Trampoline pattern.
 * Retrieves version info at runtime and uses a Staples-themed sober palette.
 */
class MainActivity : Activity() {

    private val stagingDomain = "www.staples.com"
    private val altDomain = "staples.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 128, 64, 64)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.background))
        }

        // Use a ScrollView if content exceeds screen height
        val scrollView = android.widget.ScrollView(this)
        scrollView.addView(layout)

        // Android Version Info
        layout.addView(
            TextView(this).apply {
                text = getString(R.string.android_version_label, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)
            },
        )

        // Agent Version (App Version Name)
        layout.addView(
            TextView(this).apply {
                val versionName = try {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: Exception) {
                    "Unknown"
                }
                text = getString(R.string.agent_version_label, versionName)
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 48)
            },
        )

        layout.addView(
            TextView(this).apply {
                text = getString(R.string.test_harness_title)
                textSize = 20f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 80)
            },
        )

        val buttonSpacing = 32
        layout.addView(testButton("App Deep Link (www /p/123)", "https://$stagingDomain/p/123"))
        addSpacer(layout, buttonSpacing)
        layout.addView(testButton("App Deep Link (no-www /p/123)", "https://$altDomain/p/123"))
        addSpacer(layout, buttonSpacing)
        layout.addView(testButton("App Deep Link (Custom Scheme)", "staples://p/123"))
        addSpacer(layout, buttonSpacing)
        layout.addView(testButton("Auth (www /login)", "https://$stagingDomain/login"))
        addSpacer(layout, buttonSpacing)
        layout.addView(testButton("Auth (no-www /login)", "https://$altDomain/login"))
        addSpacer(layout, buttonSpacing)
        layout.addView(testButton("Browser-Only (www /unsubscribe)", "https://$stagingDomain/unsubscribe"))
        addSpacer(layout, buttonSpacing)
        layout.addView(testButton("Auth Callback (custom scheme)", "com.staples.trampolinepoc://callback?code=test123"))
        addSpacer(layout, buttonSpacing)

        layout.addView(
            TextView(this).apply {
                text = getString(R.string.system_test_title)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                setPadding(0, 48, 0, 24)
            },
        )

        layout.addView(systemTestButton("Test Unsubscribe (www)", "https://$stagingDomain/unsubscribe"))
        addSpacer(layout, buttonSpacing)
        layout.addView(systemTestButton("Test Unsubscribe (no-www)", "https://$altDomain/unsubscribe"))
        addSpacer(layout, buttonSpacing)

        setContentView(scrollView)
    }

    private fun addSpacer(layout: LinearLayout, height: Int) {
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, height)
        })
    }

    private fun testButton(label: String, url: String): Button {
        return createStyledButton(label).apply {
            setOnClickListener {
                Log.d("MainActivity", "Firing explicit test intent: $url")
                val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    setClass(this@MainActivity, TrampolineActivity::class.java)
                }
                startActivity(intent)
            }
        }
    }

    private fun systemTestButton(label: String, url: String): Button {
        return createStyledButton(label).apply {
            setOnClickListener {
                Log.d("MainActivity", "Firing generic system intent: $url")
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
            }
        }
    }

    private fun createStyledButton(label: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 16f
            
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(context, R.color.primary))
            }
            background = shape
            setPadding(32, 24, 32, 24)
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
}

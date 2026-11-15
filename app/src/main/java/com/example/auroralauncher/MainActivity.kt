package com.example.auroralauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("aurora_home", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private lateinit var clock: TextView
    private var allApps: List<AppEntry> = emptyList()

    private data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

    private val clockTick = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) {
                val now = Date()
                clock.text = SimpleDateFormat("h:mm a  •  EEEE, d MMMM", Locale.getDefault()).format(now)
            }
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        loadApps()
        buildHome()
        handler.post(clockTick)
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) { loadApps(); buildHome() }
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        super.onDestroy()
    }

    private fun loadApps() {
        val pm = packageManager
        val found = LinkedHashMap<String, AppEntry>()
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        intents.forEach { intent ->
            @Suppress("DEPRECATION")
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            activities.forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg != packageName && !found.containsKey(pkg)) {
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg.substringAfterLast('.') }
                    found[pkg] = AppEntry(label, pkg, info.loadIcon(pm))
                }
            }
        }
        allApps = found.values.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun buildHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(42), dp(24), dp(42), dp(22))
            setBackgroundColor(Color.rgb(8, 13, 24))
            isFocusableInTouchMode = true
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = text("AURORA", 19f, Color.WHITE, true)
        brand.letterSpacing = .18f
        top.addView(brand, LinearLayout.LayoutParams(0, dp(44), 1f))
        clock = text("", 15f, Color.rgb(202, 213, 231), false)
        top.addView(clock)
        root.addView(top)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(30), dp(22), dp(30), dp(22))
            background = rounded(intArrayOf(Color.rgb(29, 69, 111), Color.rgb(18, 30, 51)), 22)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        val eyebrow = text("WELCOME HOME", 12f, Color.rgb(179, 211, 248), true).apply { letterSpacing = .16f }
        val title = text("Everything you love.\nAll in one place.", 29f, Color.WHITE, true)
        val subtitle = text("Your TV, your way.", 15f, Color.rgb(208, 221, 239), false)
        hero.addView(eyebrow)
        hero.addView(title, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        hero.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(174)).apply { topMargin = dp(20); bottomMargin = dp(22) })

        val scroll = ScrollView(this).apply { isFillViewport = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val favorites = favoriteEntries()
        if (favorites.isNotEmpty()) addAppSection(content, "YOUR APPS", favorites, true)
        addAppSection(content, "ALL APPS", allApps, false)
        val hint = text("Tip: press and hold an app to add it to or remove it from Your Apps.", 12f, Color.rgb(133, 149, 173), false)
        content.addView(hint, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(10) })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestFocus()
    }

    private fun favoriteEntries(): List<AppEntry> {
        val saved = prefs.getString("favorites", "")!!.split('|').filter { it.isNotBlank() }
        val byPackage = allApps.associateBy { it.packageName }
        val ordered = saved.mapNotNull { byPackage[it] }.toMutableList()
        allApps.filter { app -> saved.contains(app.packageName).not() && prefs.getBoolean("auto_pin_${app.packageName}", false) }
            .forEach { ordered.add(it) }
        return ordered
    }

    private fun addAppSection(parent: LinearLayout, heading: String, apps: List<AppEntry>, favorites: Boolean) {
        val headingView = text(heading, 15f, Color.WHITE, true).apply { letterSpacing = .08f }
        parent.addView(headingView, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        if (apps.isEmpty()) {
            parent.addView(text("No apps found", 13f, Color.LTGRAY, false))
            return
        }
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        apps.forEach { app -> row.addView(appTile(app, favorites), LinearLayout.LayoutParams(dp(126), dp(112)).apply { rightMargin = dp(12) }) }
        horizontal.addView(row)
        parent.addView(horizontal, LinearLayout.LayoutParams(-1, dp(120)).apply { bottomMargin = dp(18) })
    }

    private fun appTile(app: AppEntry, isFavoriteRow: Boolean): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(7))
            background = rounded(intArrayOf(Color.rgb(28, 38, 56), Color.rgb(22, 30, 45)), 16)
            isFocusable = true
            isFocusableInTouchMode = true
            contentDescription = app.label
            setOnFocusChangeListener { v, focused ->
                v.background = if (focused) rounded(intArrayOf(Color.rgb(57, 91, 132), Color.rgb(39, 63, 94)), 16, Color.rgb(164, 204, 255)) else rounded(intArrayOf(Color.rgb(28, 38, 56), Color.rgb(22, 30, 45)), 16)
                v.scaleX = if (focused) 1.045f else 1f
                v.scaleY = if (focused) 1.045f else 1f
            }
            setOnClickListener { launchApp(app.packageName) }
            setOnLongClickListener { showAppOptions(app); true }
        }
        val icon = ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER }
        tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
        val label = text(app.label, 12f, Color.WHITE, true).apply { gravity = Gravity.CENTER; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        tile.addView(label, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        return tile
    }

    private fun showAppOptions(app: AppEntry) {
        val saved = prefs.getString("favorites", "")!!.split('|').filter { it.isNotBlank() }.toMutableList()
        val isPinned = saved.contains(app.packageName)
        val choices = if (isPinned) arrayOf("Remove from Your Apps", "Move left", "Move right", "Cancel") else arrayOf("Add to Your Apps", "Cancel")
        AlertDialog.Builder(this).setTitle(app.label).setItems(choices) { _, which ->
            if (!isPinned) {
                if (which == 0) saved.add(app.packageName)
            } else {
                when (which) {
                    0 -> saved.remove(app.packageName)
                    1 -> { val i = saved.indexOf(app.packageName); if (i > 0) { val v = saved.removeAt(i); saved.add(i - 1, v) } }
                    2 -> { val i = saved.indexOf(app.packageName); if (i >= 0 && i < saved.lastIndex) { val v = saved.removeAt(i); saved.add(i + 1, v) } }
                }
            }
            prefs.edit().putString("favorites", saved.joinToString("|")).apply()
            buildHome()
        }.show()
    }

    private fun launchApp(pkg: String) {
        val pm = packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        if (intent != null) try { startActivity(intent) } catch (_: Exception) { }
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun rounded(colors: IntArray, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(2), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { loadApps(); buildHome(); return true }
        return super.onKeyDown(keyCode, event)
    }
}

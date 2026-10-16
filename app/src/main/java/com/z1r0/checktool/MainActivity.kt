package com.z1r0.checktool

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var permCard: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressLabel: TextView
    private lateinit var searchSection: LinearLayout
    private lateinit var resultsBadge: TextView
    private lateinit var resultsList: LinearLayout
    @Volatile private var token = 0
    private val maxResults = 1500

    private val bg = Color.parseColor("#0A0A0A")
    private val card = Color.parseColor("#161616")
    private val accent = Color.parseColor("#F2F2F2")
    private val dim = Color.parseColor("#8A8A8A")
    private val ok = Color.parseColor("#6FE08A")
    private val warn = Color.parseColor("#E05C5C")
    private val warnBg = Color.parseColor("#2A1414")

    private val flagKeywords = listOf("hologram", "xit", "zit", "b13", "esp", "localconfig", "aim")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(48, 120, 48, 48)
        }

        val logo = TextView(this).apply {
            text = "🔎"
            textSize = 46f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(card)
                setStroke(3, Color.parseColor("#2A2A2A"))
            }
        }
        val logoWrap = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(220, 220).apply { gravity = Gravity.CENTER }
        }
        logoWrap.addView(logo, FrameLayout.LayoutParams(220, 220))
        val logoRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        logoRow.addView(logoWrap)
        root.addView(logoRow)

        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 28) })
        root.addView(TextView(this).apply {
            text = "CHECK TOOL"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.04f
        })
        root.addView(TextView(this).apply {
            text = "by Z1R0"
            textSize = 13f
            setTextColor(dim)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 36)
        })

        val progCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 32)
            background = GradientDrawable().apply { setColor(card); cornerRadius = 28f }
        }
        val progRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        progRow.addView(TextView(this).apply {
            text = "Status"; setTextColor(dim); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        progressLabel = TextView(this).apply {
            text = "0 / 1"; setTextColor(Color.WHITE); textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        progRow.addView(progressLabel)
        progCard.addView(progRow)
        progCard.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 16) })
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1; progress = 0
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 14)
        }
        progCard.addView(progressBar)
        root.addView(progCard)
        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 24) })

        permCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 32)
            background = GradientDrawable().apply { setColor(card); cornerRadius = 28f }
        }
        permCard.addView(TextView(this).apply {
            text = "Storage Permission"
            textSize = 17f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        permCard.addView(TextView(this).apply {
            text = "Needed to search files and folders on your device"
            textSize = 13f; setTextColor(dim); setPadding(0, 6, 0, 24)
        })
        permCard.addView(Button(this).apply {
            text = "GRANT"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { ensurePermission() }
        })
        root.addView(permCard)
        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 28) })

        searchSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE; alpha = 0f }
        input = EditText(this).apply {
            hint = "Search files / folders..."
            setHintTextColor(dim); setTextColor(Color.WHITE)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = GradientDrawable().apply { setColor(card); cornerRadius = 20f }
            setPadding(28, 24, 28, 24)
            setOnEditorActionListener { _, _, _ -> startSearch(input.text.toString().trim(), false); true }
        }
        searchSection.addView(input)
        searchSection.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 16) })
        searchSection.addView(Button(this).apply {
            text = "SEARCH"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { startSearch(input.text.toString().trim(), false) }
        })
        val statusRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4, 20, 4, 8) }
        status = TextView(this).apply {
            setTextColor(dim); text = "Scanning for flagged items..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        resultsBadge = TextView(this).apply {
            setTextColor(Color.BLACK); setPadding(20, 6, 20, 6)
            background = GradientDrawable().apply { setColor(ok); cornerRadius = 100f }
            visibility = View.GONE
        }
        statusRow.addView(status); statusRow.addView(resultsBadge)
        searchSection.addView(statusRow)
        resultsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        searchSection.addView(resultsList)
        root.addView(searchSection)

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        refreshPermissionUi()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
    }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private var autoScanDone = false

    private fun refreshPermissionUi() {
        if (hasPermission()) {
            progressBar.progress = 1; progressLabel.text = "1 / 1"
            permCard.visibility = View.GONE
            if (searchSection.visibility != View.VISIBLE) {
                searchSection.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(searchSection, "alpha", 0f, 1f).apply {
                    duration = 450; interpolator = AccelerateDecelerateInterpolator(); start()
                }
            }
            if (!autoScanDone) { autoScanDone = true; startSearch(null, true) }
        } else {
            progressBar.progress = 0; progressLabel.text = "0 / 1"
            permCard.visibility = View.VISIBLE
            searchSection.visibility = View.GONE
        }
    }

    private fun ensurePermission() {
        if (hasPermission()) { refreshPermissionUi(); return }
        if (Build.VERSION.SDK_INT >= 30) {
            Toast.makeText(this, "Allow 'All files access' for CHECK TOOL", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")))
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(c, p, r)
        refreshPermissionUi()
    }

    private fun addResultCard(text: String, flagged: Boolean) {
        val row = TextView(this).apply {
            this.text = text
            setTextColor(if (flagged) warn else Color.WHITE)
            textSize = 13f
            setPadding(24, 20, 24, 20)
            background = GradientDrawable().apply {
                setColor(if (flagged) warnBg else card)
                cornerRadius = 16f
            }
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, 12)
        resultsList.addView(row, lp)
    }

    private fun startSearch(customQuery: String?, isAutoFlagScan: Boolean) {
        if (!isAutoFlagScan && customQuery.isNullOrEmpty()) return
        if (!hasPermission()) { ensurePermission(); return }
        val my = ++token
        resultsList.removeAllViews()
        status.text = if (isAutoFlagScan) "Scanning for flagged items..." else "Searching..."
        resultsBadge.visibility = View.GONE
        Thread {
            val flagged = ArrayList<String>()
            val custom = ArrayList<String>()

            // MediaStore trash pass (only useful for custom query or general flag scan)
            if (Build.VERSION.SDK_INT >= 30 && !isAutoFlagScan && customQuery != null) try {
                val args = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("%$customQuery%"))
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(MediaStore.Files.getContentUri("external"),
                    arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.IS_TRASHED), args, null)?.use { c ->
                    while (c.moveToNext() && custom.size < maxResults) {
                        val p = c.getString(0) ?: continue
                        custom.add(if (c.getInt(1) == 1) "🗑️ [DELETED] $p" else "📄 $p")
                    }
                }
            } catch (_: Exception) {}

            val seen = HashSet<String>()
            val stack = ArrayDeque<File>()
            stack.add(Environment.getExternalStorageDirectory())
            while (stack.isNotEmpty() && my == token && (flagged.size + custom.size) < maxResults) {
                val dir = stack.removeLast()
                val kids = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
                for (f in kids) {
                    val n = f.name
                    val low = n.lowercase()
                    if (seen.add(f.absolutePath)) {
                        val isFlag = flagKeywords.any { low.contains(it) }
                        if (isFlag) flagged.add((if (f.isDirectory) "📁 " else "📄 ") + f.absolutePath)
                        if (!isAutoFlagScan && customQuery != null && low.contains(customQuery.lowercase())) {
                            val deletedLow = f.absolutePath.lowercase()
                            val deleted = n.startsWith(".trashed") || deletedLow.contains("/.trash") ||
                                deletedLow.contains("/recycle") || deletedLow.contains("/.recently-deleted")
                            val tag = if (deleted) "🗑️ [DELETED] " else if (f.isDirectory) "📁 " else "📄 "
                            custom.add(tag + f.absolutePath)
                        }
                    }
                    if (f.isDirectory) stack.add(f)
                }
            }
            runOnUiThread {
                if (my != token) return@runOnUiThread
                if (flagged.isNotEmpty()) {
                    val header = TextView(this).apply {
                        text = "⚠ FLAGGED (${flagged.size})"
                        setTextColor(warn); textSize = 13f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setPadding(4, 8, 4, 12)
                    }
                    resultsList.addView(header)
                    flagged.forEach { addResultCard(it, true) }
                }
                if (!isAutoFlagScan) {
                    if (custom.isNotEmpty()) {
                        val header = TextView(this).apply {
                            text = "RESULTS (${custom.size})"
                            setTextColor(dim); textSize = 13f
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            setPadding(4, 16, 4, 12)
                        }
                        resultsList.addView(header)
                        custom.forEach { addResultCard(it, false) }
                    }
                    status.text = if (custom.isEmpty()) "No matches found" else "Search complete"
                    resultsBadge.text = "${custom.size}" + if (custom.size >= maxResults) "+" else ""
                    resultsBadge.visibility = View.VISIBLE
                } else {
                    status.text = if (flagged.isEmpty()) "No flagged items found" else "Flag scan complete — review above"
                    if (flagged.isNotEmpty()) {
                        resultsBadge.text = "${flagged.size}"
                        resultsBadge.background = GradientDrawable().apply { setColor(warn); cornerRadius = 100f }
                        resultsBadge.visibility = View.VISIBLE
                    }
                }
            }
        }.start()
    }
}

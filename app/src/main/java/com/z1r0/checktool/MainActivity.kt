package com.z1r0.checktool

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
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
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var permCard: LinearLayout
    private lateinit var grantButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressLabel: TextView
    private lateinit var searchSection: LinearLayout
    private lateinit var resultsBadge: TextView
    private lateinit var resultsList: LinearLayout
    private lateinit var statusCard: LinearLayout
    private lateinit var statusCardTitle: TextView
    private lateinit var statusCardSub: TextView
    private lateinit var logoWrap: FrameLayout
    private lateinit var progCard: LinearLayout
    private lateinit var gateCard: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var requestButton: Button
    private lateinit var gateStatus: TextView
    private lateinit var prefs: SharedPreferences
    private var requestListener: ListenerRegistration? = null
    private val auth by lazy { FirebaseAuth.getInstance() }
    @Volatile private var token = 0
    private val maxResults = 1500
    private var pulseAnimator: ObjectAnimator? = null
    private var breatheAnimator: ObjectAnimator? = null
    private var dotsHandler: Handler? = null
    private var dotsRunnable: Runnable? = null

    private val bg = Color.parseColor("#07080A")
    private val glassFill = Color.parseColor("#26FFFFFF")
    private val glassFillSoft = Color.parseColor("#14FFFFFF")
    private val glassStroke = Color.parseColor("#40FFFFFF")
    private val accent = Color.parseColor("#F2F2F2")
    private val dim = Color.parseColor("#9AA0A6")
    private val ok = Color.parseColor("#6FE08A")
    private val okGlass = Color.parseColor("#406FE08A")
    private val warn = Color.parseColor("#E05C5C")
    private val warnGlass = Color.parseColor("#40E05C5C")

    private val flagKeywords = listOf(
        "hologram", "xit", "zit", "b13", "esp", "localconfig", "aim",
        "cheat", "mod_menu", "modmenu", "injector", "wallhack", "aimbot",
        "gg.", "gameguardian", "lucky patcher", "luckypatcher", "xmod",
        "speedhack", "bypass", "triggerbot"
    )

    private fun glassBg(fill: Int = glassFill, radius: Float = 28f, strokeColor: Int = glassStroke): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius
            setStroke(2, strokeColor)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val backdrop = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#141821"), Color.parseColor("#07080A"), Color.parseColor("#0A0506"))
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = backdrop
            setPadding(48, 120, 48, 48)
        }

        val logo = TextView(this).apply {
            text = "🛡️"
            textSize = 46f
            gravity = Gravity.CENTER
            background = glassBg(glassFillSoft, 110f, glassStroke)
        }
        logoWrap = FrameLayout(this).apply {
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
            letterSpacing = 0.05f
        })
        root.addView(TextView(this).apply {
            text = "by Z1R0"
            textSize = 13f
            setTextColor(dim)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 36)
        })

        gateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 32)
            background = glassBg()
        }
        gateCard.addView(TextView(this).apply {
            text = "Request Access"
            textSize = 17f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        gateCard.addView(TextView(this).apply {
            text = "Enter your name and request approval before scanning"
            textSize = 13f; setTextColor(dim); setPadding(0, 6, 0, 20)
        })
        nameInput = EditText(this).apply {
            hint = "Your name"
            setHintTextColor(dim); setTextColor(Color.WHITE)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            background = glassBg(glassFillSoft, 20f, glassStroke)
            setPadding(28, 24, 28, 24)
            setOnEditorActionListener { _, _, _ -> submitAccessRequest(); true }
        }
        gateCard.addView(nameInput)
        gateCard.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 16) })
        requestButton = Button(this).apply {
            text = "REQUEST ACCESS"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { submitAccessRequest() }
        }
        gateCard.addView(requestButton)
        gateStatus = TextView(this).apply {
            text = ""; textSize = 12f; setTextColor(dim)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        gateCard.addView(gateStatus)
        root.addView(gateCard)
        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 24) })

        progCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 32)
            background = glassBg()
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
            max = 100; progress = 0
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 14)
        }
        progCard.addView(progressBar)
        root.addView(progCard)
        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 24) })
        progCard.visibility = View.GONE

        permCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 32)
            background = glassBg()
        }
        permCard.addView(TextView(this).apply {
            text = "Storage Permission"
            textSize = 17f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        permCard.addView(TextView(this).apply {
            text = "Needed to scan files and installed apps on your device"
            textSize = 13f; setTextColor(dim); setPadding(0, 6, 0, 24)
        })
        grantButton = Button(this).apply {
            text = "GRANT"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { ensurePermission() }
        }
        permCard.addView(grantButton)
        root.addView(permCard)
        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 28) })
        permCard.visibility = View.GONE

        searchSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE; alpha = 0f }

        statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 48, 36, 48)
            background = glassBg()
        }
        statusCardTitle = TextView(this).apply {
            text = "SCANNING"
            textSize = 24f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        statusCardSub = TextView(this).apply {
            text = "Checking device for cheat-related files and apps"
            textSize = 12f; setTextColor(dim)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }
        statusCard.addView(statusCardTitle)
        statusCard.addView(statusCardSub)
        searchSection.addView(statusCard)
        searchSection.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 28) })

        input = EditText(this).apply {
            hint = "Search files / folders..."
            setHintTextColor(dim); setTextColor(Color.WHITE)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = glassBg(glassFillSoft, 20f, glassStroke)
            setPadding(28, 24, 28, 24)
            setOnEditorActionListener { _, _, _ -> startFileSearch(input.text.toString().trim()); true }
        }
        searchSection.addView(input)
        searchSection.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 16) })
        searchSection.addView(Button(this).apply {
            text = "SEARCH FILES"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { startFileSearch(input.text.toString().trim()) }
        })
        val statusRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4, 20, 4, 8) }
        status = TextView(this).apply {
            setTextColor(dim); text = ""
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

        val scroll = ScrollView(this).apply { background = backdrop; addView(root) }
        setContentView(scroll)
        window.decorView.setBackgroundColor(bg)
        startBreathe()

        prefs = getSharedPreferences("checktool_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("last_name", "")
        if (!savedName.isNullOrBlank()) nameInput.setText(savedName)

        if (prefs.getBoolean("approved", false)) {
            unlockMainFlow()
        } else {
            gateCard.visibility = View.VISIBLE
            // Resume watching an already-submitted request (e.g. app was closed while pending).
            val existingId = prefs.getString("device_id", null)
            if (existingId != null) {
                requestButton.isEnabled = false
                requestButton.text = "WAITING FOR APPROVAL"
                gateStatus.text = "Waiting for approval..."
                listenForApproval(existingId)
            }
        }
    }

    private fun unlockMainFlow() {
        gateCard.visibility = View.GONE
        progCard.visibility = View.VISIBLE
        permCard.visibility = View.VISIBLE
        refreshPermissionUi()
        startPulse()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("approved", false)) refreshPermissionUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        dotsHandler?.removeCallbacksAndMessages(null)
        requestListener?.remove()
    }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private var autoScanDone = false
    private var wasGrantedBefore = false

    private fun startPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofFloat(grantButton, "alpha", 1f, 0.6f, 1f).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        grantButton.alpha = 1f
    }

    private fun startBreathe() {
        breatheAnimator?.cancel()
        val sx = ObjectAnimator.ofFloat(logoWrap, "scaleX", 1f, 1.07f, 1f)
        val sy = ObjectAnimator.ofFloat(logoWrap, "scaleY", 1f, 1.07f, 1f)
        sx.duration = 2200; sy.duration = 2200
        sx.repeatCount = ValueAnimator.INFINITE; sy.repeatCount = ValueAnimator.INFINITE
        sx.interpolator = AccelerateDecelerateInterpolator(); sy.interpolator = AccelerateDecelerateInterpolator()
        sx.start(); sy.start()
        breatheAnimator = sx
    }

    private fun animateProgressTo(target: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, target).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startScanningDots() {
        dotsHandler?.removeCallbacksAndMessages(null)
        dotsHandler = Handler(Looper.getMainLooper())
        var count = 0
        dotsRunnable = object : Runnable {
            override fun run() {
                count = (count + 1) % 4
                statusCardTitle.text = "SCANNING" + ".".repeat(count)
                dotsHandler?.postDelayed(this, 400)
            }
        }
        dotsHandler?.post(dotsRunnable!!)
    }

    private fun stopScanningDots() {
        dotsHandler?.removeCallbacksAndMessages(null)
    }

    private fun refreshPermissionUi() {
        if (hasPermission()) {
            animateProgressTo(100)
            progressLabel.text = "1 / 1"
            stopPulse()
            if (!wasGrantedBefore) {
                wasGrantedBefore = true
                Toast.makeText(this, "✓ Permission granted", Toast.LENGTH_SHORT).show()
            }
            permCard.visibility = View.GONE
            if (searchSection.visibility != View.VISIBLE) {
                searchSection.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(searchSection, "alpha", 0f, 1f).apply {
                    duration = 450; interpolator = AccelerateDecelerateInterpolator(); start()
                }
            }
            if (!autoScanDone) { autoScanDone = true; runFullScan() }
        } else {
            animateProgressTo(0)
            progressLabel.text = "0 / 1"
            permCard.visibility = View.VISIBLE
            searchSection.visibility = View.GONE
            startPulse()
        }
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    private fun submitAccessRequest() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name first", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit().putString("last_name", name).apply()
        requestButton.isEnabled = false
        requestButton.text = "REQUESTING..."
        gateStatus.setTextColor(dim)
        gateStatus.text = "Sending request..."
        val deviceId = getOrCreateDeviceId()
        val data = hashMapOf(
            "name" to name,
            "deviceId" to deviceId,
            "status" to "pending",
            "requestedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("requests").document(deviceId).set(data)
            .addOnSuccessListener { listenForApproval(deviceId) }
            .addOnFailureListener { e ->
                requestButton.isEnabled = true
                requestButton.text = "REQUEST ACCESS"
                gateStatus.setTextColor(warn)
                gateStatus.text = "Could not send request: ${e.message}"
            }
    }

    private fun listenForApproval(deviceId: String) {
        requestListener?.remove()
        requestListener = db.collection("requests").document(deviceId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                when (snap.getString("status")) {
                    "pending" -> {
                        requestButton.isEnabled = false
                        requestButton.text = "WAITING FOR APPROVAL"
                        gateStatus.setTextColor(dim)
                        gateStatus.text = "Waiting for approval..."
                    }
                    "approved" -> {
                        prefs.edit().putBoolean("approved", true).apply()
                        requestListener?.remove()
                        unlockMainFlow()
                    }
                    "declined" -> {
                        requestButton.isEnabled = true
                        requestButton.text = "REQUEST ACCESS"
                        gateStatus.setTextColor(warn)
                        gateStatus.text = "Request declined. You can try again."
                    }
                }
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

    private fun addResultCard(path: String, flagged: Boolean, delayMs: Long) {
        val timeStr = try {
            val cleanPath = path.substringAfter("🗑️ [DELETED] ").substringAfter("📁 ").substringAfter("📄 ")
            val f = File(cleanPath)
            if (f.exists()) {
                val sdf = java.text.SimpleDateFormat("MMM d, yyyy — HH:mm", java.util.Locale.getDefault())
                "Last modified: " + sdf.format(java.util.Date(f.lastModified()))
            } else "Last modified: unknown"
        } catch (_: Exception) { "Last modified: unknown" }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            alpha = 0f
            translationY = 20f
            background = glassBg(if (flagged) warnGlass else glassFillSoft, 16f, if (flagged) Color.parseColor("#60E05C5C") else glassStroke)
        }
        container.addView(TextView(this).apply {
            text = path
            setTextColor(if (flagged) Color.parseColor("#FF9D9D") else Color.WHITE)
            textSize = 13f
        })
        container.addView(TextView(this).apply {
            text = timeStr
            setTextColor(dim)
            textSize = 11f
            setPadding(0, 6, 0, 0)
        })
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, 12)
        resultsList.addView(container, lp)
        container.animate().alpha(1f).translationY(0f).setStartDelay(delayMs).setDuration(260).start()
    }

    private fun addHeader(text: String, color: Int) {
        resultsList.addView(TextView(this).apply {
            this.text = text; setTextColor(color); textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(4, 16, 4, 12)
        })
    }

    private fun setStatusCard(clean: Boolean, count: Int) {
        stopScanningDots()
        statusCard.alpha = 0f
        statusCard.background = glassBg(if (clean) okGlass else warnGlass, 28f, if (clean) Color.parseColor("#606FE08A") else Color.parseColor("#60E05C5C"))
        statusCardTitle.setTextColor(if (clean) ok else warn)
        statusCardTitle.text = if (clean) "✓ CLEAN" else "⚠ FLAGGED ($count)"
        statusCardSub.text = if (clean) "No cheat-related files or apps found"
            else "Suspicious files or apps were detected — review below"
        statusCard.animate().alpha(1f).setDuration(350).start()
    }

    private fun runFullScan() {
        val my = ++token
        resultsList.removeAllViews()
        statusCardTitle.text = "SCANNING"
        statusCardSub.text = "Checking device for cheat-related files and apps"
        startScanningDots()
        status.text = ""
        Thread {
            val flaggedApps = ArrayList<String>()
            try {
                val pkgs = packageManager.getInstalledApplications(0)
                for (ai in pkgs) {
                    val pkgName = ai.packageName.lowercase()
                    val label = try { packageManager.getApplicationLabel(ai).toString() } catch (_: Exception) { ai.packageName }
                    if (flagKeywords.any { pkgName.contains(it.replace(" ", "")) || label.lowercase().contains(it) }) {
                        flaggedApps.add("📱 $label  ($pkgName)")
                    }
                }
            } catch (_: Exception) {}

            val flaggedFiles = ArrayList<String>()
            val seen = HashSet<String>()
            val stack = ArrayDeque<File>()
            stack.add(Environment.getExternalStorageDirectory())
            while (stack.isNotEmpty() && my == token && flaggedFiles.size < maxResults) {
                val dir = stack.removeLast()
                val kids = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
                for (f in kids) {
                    val low = f.name.lowercase()
                    if (seen.add(f.absolutePath) && flagKeywords.any { low.contains(it) }) {
                        flaggedFiles.add((if (f.isDirectory) "📁 " else "📄 ") + f.absolutePath)
                    }
                    if (f.isDirectory) stack.add(f)
                }
            }

            runOnUiThread {
                if (my != token) return@runOnUiThread
                val total = flaggedApps.size + flaggedFiles.size
                setStatusCard(total == 0, total)
                var delay = 0L
                if (flaggedApps.isNotEmpty()) {
                    addHeader("⚠ SUSPICIOUS APPS (${flaggedApps.size})", warn)
                    flaggedApps.forEach { addResultCard(it, true, delay); delay += 60 }
                }
                if (flaggedFiles.isNotEmpty()) {
                    addHeader("⚠ SUSPICIOUS FILES (${flaggedFiles.size})", warn)
                    flaggedFiles.forEach { addResultCard(it, true, delay); delay += 60 }
                }
            }
        }.start()
    }

    private fun startFileSearch(query: String) {
        if (query.isEmpty()) return
        if (!hasPermission()) { ensurePermission(); return }
        val my = ++token
        resultsList.removeAllViews()
        status.text = "Searching..."
        resultsBadge.visibility = View.GONE
        Thread {
            val custom = ArrayList<String>()
            if (Build.VERSION.SDK_INT >= 30) try {
                val args = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("%$query%"))
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
            val seen = HashSet<String>(custom.map { it.substringAfter("] ").substringAfter("📄 ") })
            val stack = ArrayDeque<File>()
            stack.add(Environment.getExternalStorageDirectory())
            while (stack.isNotEmpty() && my == token && custom.size < maxResults) {
                val dir = stack.removeLast()
                val kids = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
                for (f in kids) {
                    val low = f.name.lowercase()
                    if (low.contains(query.lowercase()) && seen.add(f.absolutePath)) {
                        val tag = if (f.isDirectory) "📁 " else "📄 "
                        custom.add(tag + f.absolutePath)
                    }
                    if (f.isDirectory) stack.add(f)
                }
            }
            runOnUiThread {
                if (my != token) return@runOnUiThread
                if (custom.isNotEmpty()) {
                    addHeader("RESULTS (${custom.size})", dim)
                    var delay = 0L
                    custom.forEach { addResultCard(it, false, delay); delay += 50 }
                }
                status.text = if (custom.isEmpty()) "No matches found" else "Search complete"
                resultsBadge.text = "${custom.size}" + if (custom.size >= maxResults) "+" else ""
                resultsBadge.background = GradientDrawable().apply { setColor(ok); cornerRadius = 100f }
                resultsBadge.visibility = View.VISIBLE
            }
        }.start()
    }
}

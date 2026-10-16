package com.z1r0.checktool

import android.Manifest
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
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var permCard: LinearLayout
    private lateinit var grantBtn: Button
    private lateinit var searchSection: LinearLayout
    private val results = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    @Volatile private var token = 0
    private val maxResults = 3000

    private val bg = Color.parseColor("#0A0A0A")
    private val card = Color.parseColor("#161616")
    private val accent = Color.parseColor("#E0E0E0")
    private val dim = Color.parseColor("#8A8A8A")
    private val danger = Color.parseColor("#E05C5C")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(48, 110, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "CHECK TOOL"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "by Z1R0"
            textSize = 13f
            setTextColor(dim)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 36)
        })

        // Permission card
        permCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 32)
            background = GradientDrawable().apply {
                setColor(card); cornerRadius = 28f
            }
        }
        permCard.addView(TextView(this).apply {
            text = "Storage Permission"
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        permCard.addView(TextView(this).apply {
            text = "Needed to search files and folders on your device"
            textSize = 13f
            setTextColor(dim)
            setPadding(0, 6, 0, 24)
        })
        grantBtn = Button(this).apply {
            text = "GRANT"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { ensurePermission() }
        }
        permCard.addView(grantBtn)
        root.addView(permCard)
        root.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 32) })

        // Search section (built, shown once permission is granted)
        searchSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        input = EditText(this).apply {
            hint = "Search files / folders..."
            setHintTextColor(dim)
            setTextColor(Color.WHITE)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = GradientDrawable().apply {
                setColor(card); cornerRadius = 20f
            }
            setPadding(28, 24, 28, 24)
            setOnEditorActionListener { _, _, _ -> startSearch(); true }
        }
        searchSection.addView(input)
        searchSection.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 16) })
        searchSection.addView(Button(this).apply {
            text = "SEARCH"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply { setColor(accent); cornerRadius = 20f }
            setOnClickListener { startSearch() }
        })
        status = TextView(this).apply { setTextColor(dim); setPadding(4, 16, 0, 8) }
        searchSection.addView(status)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, results)
        searchSection.addView(ListView(this).apply { this.adapter = this@MainActivity.adapter })
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

    private fun refreshPermissionUi() {
        if (hasPermission()) {
            permCard.visibility = View.GONE
            searchSection.visibility = View.VISIBLE
        } else {
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

    private fun startSearch() {
        val q = input.text.toString().trim()
        if (q.isEmpty()) return
        if (!hasPermission()) { ensurePermission(); return }
        val my = ++token
        results.clear(); adapter.notifyDataSetChanged(); status.text = "Searching..."
        Thread {
            val found = ArrayList<String>()
            if (Build.VERSION.SDK_INT >= 30) try {
                val args = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("%$q%"))
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(MediaStore.Files.getContentUri("external"),
                    arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.IS_TRASHED), args, null)?.use { c ->
                    while (c.moveToNext() && found.size < maxResults) {
                        val p = c.getString(0) ?: continue
                        found.add(if (c.getInt(1) == 1) "[DELETED - in trash] $p" else p)
                    }
                }
            } catch (_: Exception) {}
            val seen = HashSet<String>(found.map { it.substringAfter("] ") })
            val stack = ArrayDeque<File>()
            stack.add(Environment.getExternalStorageDirectory())
            while (stack.isNotEmpty() && found.size < maxResults && my == token) {
                val dir = stack.removeLast()
                val kids = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
                for (f in kids) {
                    val n = f.name
                    if (n.contains(q, ignoreCase = true) && seen.add(f.absolutePath)) {
                        val low = f.absolutePath.lowercase()
                        val deleted = n.startsWith(".trashed") || low.contains("/.trash") ||
                            low.contains("/recycle") || low.contains("/.recently-deleted")
                        found.add((if (deleted) "[DELETED - in trash] " else if (f.isDirectory) "[FOLDER] " else "") + f.absolutePath)
                    }
                    if (f.isDirectory) stack.add(f)
                }
            }
            runOnUiThread {
                if (my != token) return@runOnUiThread
                results.addAll(found); adapter.notifyDataSetChanged()
                status.text = "${found.size} result(s)" + if (found.size >= maxResults) " (limit reached)" else ""
            }
        }.start()
    }
}

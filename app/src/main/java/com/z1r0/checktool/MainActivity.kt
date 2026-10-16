package com.z1r0.checktool

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private val results = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    @Volatile private var token = 0
    private val maxResults = 3000

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0E1116")); setPadding(32, 64, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "CHECK TOOL"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Credit: Z1R0 & OUSSAMA"; textSize = 13f
            setTextColor(Color.parseColor("#7EE787")); gravity = Gravity.CENTER; setPadding(0, 0, 0, 24)
        })
        input = EditText(this).apply {
            hint = "Search files / folders..."; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            isSingleLine = true; imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, _, _ -> startSearch(); true }
        }
        root.addView(input)
        root.addView(Button(this).apply { text = "SEARCH"; setOnClickListener { startSearch() } })
        status = TextView(this).apply { setTextColor(Color.LTGRAY); setPadding(0, 8, 0, 8) }
        root.addView(status)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, results)
        root.addView(ListView(this).apply { this.adapter = this@MainActivity.adapter })
        setContentView(root)
        ensurePermission()
    }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun ensurePermission() {
        if (hasPermission()) return
        if (Build.VERSION.SDK_INT >= 30) {
            Toast.makeText(this, "Allow 'All files access' for CHECK TOOL", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")))
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
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

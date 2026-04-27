package com.floatrecorder

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        val toolbar = androidx.appcompat.widget.Toolbar(this).apply {
            setBackgroundColor(0xFF4CAF50.toInt())
            title = "📁 My Recordings"
            setTitleTextColor(0xFFFFFFFF.toInt())
        }
        layout.addView(toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "FloatRecordings")
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            val empty = TextView(this).apply {
                text = "📭 No recordings yet.\n\nTap the 🎙 floating button during a call\nor use the big button on the main screen!"
                gravity = android.view.Gravity.CENTER
                setPadding(48, 100, 48, 48)
                textSize = 15f
                setTextColor(0xFF9E9E9E.toInt())
            }
            layout.addView(empty)
        } else {
            val recycler = RecyclerView(this).apply {
                layoutManager = LinearLayoutManager(this@RecordingsActivity)
            }
            layout.addView(recycler)

            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                    object : RecyclerView.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(android.R.layout.simple_list_item_2, parent, false)
                    ) {}

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val file = files[position]
                    val date = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
                        .format(Date(file.lastModified()))
                    val size = if (file.length() > 1024 * 1024)
                        "%.1f MB".format(file.length() / (1024f * 1024f))
                    else "${file.length() / 1024} KB"

                    holder.itemView.findViewById<TextView>(android.R.id.text1).text = "🎙 ${file.name}"
                    holder.itemView.findViewById<TextView>(android.R.id.text2).text = "$date  •  $size"

                    holder.itemView.setOnClickListener {
                        AlertDialog.Builder(this@RecordingsActivity)
                            .setTitle("Recording Options")
                            .setItems(arrayOf("▶️ Play", "🗑️ Delete")) { _, which ->
                                when (which) {
                                    0 -> playFile(file)
                                    1 -> {
                                        file.delete()
                                        Toast.makeText(this@RecordingsActivity, "Deleted!", Toast.LENGTH_SHORT).show()
                                        recreate()
                                    }
                                }
                            }.show()
                    }
                }

                override fun getItemCount() = files.size
            }
        }

        setContentView(layout)
    }

    private fun playFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Play with..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No audio player found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

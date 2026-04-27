package com.floatrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.floatrecorder.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private val PERMISSION_CODE = 100
    private val OVERLAY_CODE = 101

    private val PERMISSIONS = mutableListOf(
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        // Big record button
        binding.btnRecord.setOnClickListener {
            if (!allPermissionsGranted()) {
                checkPermissions()
                return@setOnClickListener
            }
            if (isRecording) stopRecording() else startRecording()
        }

        // Enable floating button
        binding.btnFloating.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivityForResult(intent, OVERLAY_CODE)
            } else {
                startFloatingService()
                Toast.makeText(this, "✅ Floating button enabled! It will appear during calls.", Toast.LENGTH_LONG).show()
            }
        }

        // View recordings
        binding.btnViewRecordings.setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }
    }

    private fun startRecording() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = getRecordingsDir()
            outputFile = File(dir, "rec_$timestamp.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }

            mediaRecorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            binding.btnRecord.text = "⏹ STOP\nRECORDING"
            binding.btnRecord.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFFF44336.toInt())
            binding.tvStatus.text = "🔴 Recording in progress..."
            binding.tvStatus.setTextColor(0xFFF44336.toInt())

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            binding.btnRecord.text = "🎙 START\nRECORDING"
            binding.btnRecord.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
            binding.tvStatus.text = "✅ Recording saved!"
            binding.tvStatus.setTextColor(0xFF2E7D32.toInt())

            Toast.makeText(this, "✅ Recording saved to ${outputFile?.name}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startForegroundService(intent)
    }

    private fun checkPermissions() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_CODE)
        } else {
            binding.tvStatus.text = "✅ Ready to record!"
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (allPermissionsGranted()) {
            binding.tvStatus.text = "✅ Ready to record!"
        } else {
            Toast.makeText(this, "Microphone permission is required!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
                Toast.makeText(this, "✅ Floating button enabled!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    fun getRecordingsDir(): File {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "FloatRecordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun onResume() {
        super.onResume()
        if (!isRecording) {
            binding.tvStatus.text = "✅ Ready to record!"
        }
    }
}

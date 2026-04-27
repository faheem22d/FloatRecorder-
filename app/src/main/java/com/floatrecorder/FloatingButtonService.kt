package com.floatrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatBtn: TextView? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null

    companion object {
        private const val CHANNEL_ID = "float_ch"
        private const val NOTIF_ID = 3001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif("🎙 Tap button to record"))
        showFloatingButton()
    }

    private fun showFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatBtn = TextView(this).apply {
            text = "🎙 REC"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC2E7D32"))
            setPadding(24, 16, 24, 16)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200
        }

        var startX = 0; var startY = 0
        var startTX = 0f; var startTY = 0f
        var dragged = false

        floatBtn!!.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    startTX = e.rawX; startTY = e.rawY
                    dragged = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - startTX).toInt()
                    val dy = (e.rawY - startTY).toInt()
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        dragged = true
                        params.x = startX - dx
                        params.y = startY + dy
                        windowManager?.updateViewLayout(floatBtn, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) toggleRecording()
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(floatBtn, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRec()
            floatBtn?.text = "🎙 REC"
            floatBtn?.setBackgroundColor(Color.parseColor("#CC2E7D32"))
            updateNotif("🎙 Tap button to record")
        } else {
            startRec()
            floatBtn?.text = "⏹ STOP"
            floatBtn?.setBackgroundColor(Color.parseColor("#CCB71C1C"))
            updateNotif("🔴 Recording... tap STOP to finish")
        }
    }

    private fun startRec() {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "FloatRecordings")
            if (!dir.exists()) dir.mkdirs()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(dir, "rec_$ts.m4a")

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()

            recorder!!.apply {
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
        } catch (e: Exception) {
            isRecording = false
            outputFile?.delete()
        }
    }

    private fun stopRec() {
        try { recorder?.stop() } catch (_: Exception) { outputFile?.delete() }
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        isRecording = false
    }

    private fun buildNotif(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FloatRecorder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotif(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(text))
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "FloatRecorder", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRec()
        try { floatBtn?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

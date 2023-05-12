package com.alterpat.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.alterpat.voicerecorder.databinding.ActivityMainBinding
import com.alterpat.voicerecorder.db.AppDatabase
import com.alterpat.voicerecorder.db.AudioRecord
import com.alterpat.voicerecorder.tools.Timer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity(), BottomSheet.OnClickListener, Timer.OnTimerUpdateListener {
    private lateinit var binding: ActivityMainBinding 
    private lateinit var fileName: String
    private lateinit var dirPath: String
    private var recorder: MediaRecorder? = null
    private var recording = false
    private var onPause = false
    private var refreshRate: Long = 60
    private lateinit var timer: Timer

    private lateinit var handler: Handler

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Record to the external cache directory for visibility
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        handler = Handler(Looper.myLooper()!!)

        binding.recordBtn.setOnClickListener {

            when {
                onPause -> resumeRecording()
                recording -> pauseRecording()
                else -> startRecording()
            }
        }

        binding.doneBtn.setOnClickListener {
            stopRecording()
            showBottomSheet()
        }

        binding.listBtn.setOnClickListener {
            startActivity(Intent(this, ListingActivity::class.java))
        }

        binding.deleteBtn.setOnClickListener {
            stopRecording()

            File(dirPath + fileName).delete()
        }
        binding.deleteBtn.isClickable = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        //if (!permissionToRecordAccepted) finish()
    }

    private fun startRecording() {

        if (!permissionToRecordAccepted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        binding.listBtn.visibility = View.GONE
        binding.doneBtn.visibility = View.VISIBLE
        binding.deleteBtn.isClickable = true
        binding.deleteBtn.setImageResource(R.drawable.ic_delete_enabled)

        recording = true
        timer = Timer(this)
        timer.start()

        // format file name with date
        val pattern = "yyyy.MM.dd_hh.mm.ss"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val date: String = simpleDateFormat.format(Date())

        dirPath = "${externalCacheDir?.absolutePath}/"
        fileName = "voice_record_${date}.mp3"

        recorder =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96000)
            setAudioSamplingRate(44100)
            setOutputFile(dirPath + fileName)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }

        binding.recordBtn.setImageResource(R.drawable.ic_pause)

        animatePlayerView()

    }

    private fun animatePlayerView() {
        if (recording && !onPause) {
            val amp = recorder!!.maxAmplitude
            binding.playerView.updateAmps(amp)

            // write maxmap to a file for visualization in player activity

            handler.postDelayed(
                Runnable {
                    kotlin.run { animatePlayerView() }
                }, refreshRate
            )
        }
    }

    private fun pauseRecording() {
        onPause = true
        recorder?.apply {
            pause()
        }
        binding.recordBtn.setImageResource(R.drawable.ic_record)
        timer.pause()

    }

    private fun resumeRecording() {
        onPause = false
        recorder?.apply {
            resume()
        }
        binding.recordBtn.setImageResource(R.drawable.ic_pause)
        animatePlayerView()
        timer.start()
    }

    private fun stopRecording() {
        recording = false
        onPause = false
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        binding.recordBtn.setImageResource(R.drawable.ic_record)

        binding.listBtn.visibility = View.VISIBLE
        binding.doneBtn.visibility = View.GONE
        binding.deleteBtn.isClickable = false
        binding.deleteBtn.setImageResource(R.drawable.ic_delete_disabled)

        binding.playerView.reset()
        try {
            timer.stop()
        } catch (_: Exception) {
        }

        binding.timerView.text = "00:00.00"
    }

    private fun showBottomSheet() {
        val bottomSheet = BottomSheet(dirPath, fileName, this)
        bottomSheet.show(supportFragmentManager, LOG_TAG)

    }


    override fun onCancelClicked() {
        Toast.makeText(this, "Audio record deleted", Toast.LENGTH_SHORT).show()
        stopRecording()
    }

    override fun onOkClicked(filePath: String, filename: String) {
        // add audio record info to database
        val db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        val duration = timer.format().split(".")[0]
        stopRecording()

        GlobalScope.launch {
            db.audioRecordDAO().insert(AudioRecord(filename, filePath, Date().time, duration))
        }

    }

    override fun onTimerUpdate(duration: String) {
        runOnUiThread {
            if (recording)
                binding.timerView.text = duration
        }
    }
}
package com.alterpat.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.alterpat.voicerecorder.db.AppDatabase
import com.alterpat.voicerecorder.db.AudioRecord
import com.alterpat.voicerecorder.tools.Timer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity(), BottomSheet.OnClickListener, Timer.OnTimerUpdateListener {

    private lateinit var fileName: String
    private lateinit var dirPath: String
    private var recorder: MediaRecorder? = null
    private var recording = false
    private var onPause = false
    private var refreshRate : Long = 60
    private lateinit var timer: Timer

    private lateinit var handler: Handler

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Record to the external cache directory for visibility
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        handler = Handler(Looper.myLooper()!!)

        recordBtn.setOnClickListener {

            when {
                onPause -> resumeRecording()
                recording -> pauseRecording()
                else -> startRecording()
            }
        }

        doneBtn.setOnClickListener {
            stopRecording()
            showBottomSheet()
        }

        listBtn.setOnClickListener {
            startActivity(Intent(this, ListingActivity::class.java))
        }

        deleteBtn.setOnClickListener {
            stopRecording()

            File(dirPath+fileName).delete()
        }
        deleteBtn.isClickable = false
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

    private fun startRecording(){

        if(!permissionToRecordAccepted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        listBtn.visibility = View.GONE
        doneBtn.visibility = View.VISIBLE
        deleteBtn.isClickable = true
        deleteBtn.setImageResource(R.drawable.ic_delete_enabled)

        recording = true
        timer = Timer(this)
        timer.start()

        // format file name with date
        val pattern = "yyyy.MM.dd_hh.mm.ss"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val date: String = simpleDateFormat.format(Date())

        dirPath = "${externalCacheDir?.absolutePath}/"
        fileName = "voice_record_${date}.mp3"

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            /** START COMMENT
             * These two together enable saving file into mp3 format
             * because android doesn't support mp3 saving explicitly **/
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            /** END COMMENT **/

            setOutputFile(dirPath+fileName)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }

        recordBtn.setImageResource(R.drawable.ic_pause)

        animatePlayerView()

    }

    private fun animatePlayerView(){
        if(recording && !onPause){
            var amp = recorder!!.maxAmplitude
            playerView.updateAmps(amp)

            // write maxmap to a file for visualization in player activity

            handler.postDelayed(
                Runnable {
                    kotlin.run { animatePlayerView() }
                }, refreshRate
            )
        }
    }

    private fun pauseRecording(){
        onPause = true
        recorder?.apply {
            pause()
        }
        recordBtn.setImageResource(R.drawable.ic_record)
        timer.pause()

    }

    private fun resumeRecording(){
        onPause = false
        recorder?.apply {
            resume()
        }
        recordBtn.setImageResource(R.drawable.ic_pause)
        animatePlayerView()
        timer.start()
    }

    private fun stopRecording(){
        recording = false
        onPause = false
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        recordBtn.setImageResource(R.drawable.ic_record)

        listBtn.visibility = View.VISIBLE
        doneBtn.visibility = View.GONE
        deleteBtn.isClickable = false
        deleteBtn.setImageResource(R.drawable.ic_delete_disabled)

        playerView.reset()
        try {
            timer.stop()
        }catch (e: Exception){}

        timerView.text = "00:00.00"
    }

    private fun showBottomSheet(){
        var bottomSheet = BottomSheet(dirPath, fileName, this)
        bottomSheet.show(supportFragmentManager, LOG_TAG)

    }



    override fun onCancelClicked() {
        Toast.makeText(this, "Audio record deleted", Toast.LENGTH_SHORT).show()
        stopRecording()
    }

    override fun onOkClicked(filePath: String, filename: String) {
        // add audio record info to database
        var db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords").build()

        var duration = timer.format().split(".")[0]
        stopRecording()

        GlobalScope.launch {
            db.audioRecordDAO().insert(AudioRecord(filename, filePath, Date().time, duration))
        }

    }

    override fun onTimerUpdate(duration: String) {
        runOnUiThread{
            if(recording)
                timerView.text = duration
        }
    }
}
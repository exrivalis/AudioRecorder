package com.alterpat.voicerecorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.activity_listing.*
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.activity_player.toolbar

class PlayerActivity : AppCompatActivity() {

    private val delay = 100L
    private lateinit var runnable : Runnable
    private lateinit var handler : Handler
    private lateinit var mediaPlayer : MediaPlayer
    private var playbackSpeed :Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        var filePath = intent.getStringExtra("filepath")
        var filename = intent.getStringExtra("filename")

        tvFilename.text = filename

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }
        seekBar.max = mediaPlayer.duration

        handler = Handler(Looper.getMainLooper())
        playPausePlayer()

        mediaPlayer.setOnCompletionListener {
            stopPlayer()
        }

        btnPlay.setOnClickListener {
            playPausePlayer()
        }

        btnForward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + 1000)
            seekBar.progress += 1000
        }

        btnBackward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition - 1000)
            seekBar.progress -= 1000

        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) mediaPlayer.seekTo(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })

        chip.setOnClickListener {
            when(playbackSpeed){
                0.5f -> playbackSpeed += 0.5f
                1.0f -> playbackSpeed += 0.5f
                1.5f -> playbackSpeed += 0.5f
                2.0f -> playbackSpeed = 0.5f
            }
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            chip.text = "x $playbackSpeed"
        }
    }

    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle, theme)

            runnable = Runnable {
                var progress = mediaPlayer.currentPosition
                Log.d("progress", progress.toString())
                seekBar.progress = progress

                var amp = 80 + Math.random()*300
                playerView.updateAmps(amp.toInt())

                handler.postDelayed(runnable, delay)
            }
            handler.postDelayed(runnable, delay)
        }else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)

            handler.removeCallbacks(runnable)
        }
    }

    private fun stopPlayer(){
        btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
        handler.removeCallbacks(runnable)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }
}
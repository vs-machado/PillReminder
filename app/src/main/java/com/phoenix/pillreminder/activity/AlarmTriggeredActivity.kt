package com.phoenix.pillreminder.activity

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityAlarmTriggeredBinding

class AlarmTriggeredActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmTriggeredBinding
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAlarmTriggeredBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()


        binding.apply{

            btnPause.setOnClickListener {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
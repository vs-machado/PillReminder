package com.phoenix.pillreminder.activity

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityAlarmTriggeredBinding

class AlarmTriggeredActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmTriggeredBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAlarmTriggeredBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        var mediaPlayer = MediaPlayer.create(applicationContext, R.raw.alarm_sound)
        mediaPlayer.isLooping = true
        mediaPlayer?.start()


        binding.apply{

            btnPause.setOnClickListener {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

}
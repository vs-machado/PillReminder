package com.phoenix.pillreminder.activity

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.databinding.ActivityAlarmTriggeredBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.AlarmTriggeredViewModel

class AlarmTriggeredActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmTriggeredBinding
    private var mediaPlayer: MediaPlayer? = null
    private val viewModel: AlarmTriggeredViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAlarmTriggeredBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        binding.apply{
            val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
            }else{
                intent?.getParcelableExtra<AlarmItem>("ALARM_ITEM")
            }

            if (alarmItem != null) {
                    tvAlarmMedicineName.text = "${alarmItem.medicineName}"
                    tvAlarmHourMedicine.text = viewModel.checkDateFormat(alarmItem.alarmHour.toInt(), alarmItem.alarmMinute.toInt(), context = applicationContext)
                    tvAlarmQuantity.text = "Take ${alarmItem.medicineQuantity} ${alarmItem.medicineForm}"
            }

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
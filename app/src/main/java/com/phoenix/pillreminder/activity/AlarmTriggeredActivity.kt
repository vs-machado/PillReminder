package com.phoenix.pillreminder.activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.databinding.ActivityAlarmTriggeredBinding
import com.phoenix.pillreminder.db.MedicineDao
import com.phoenix.pillreminder.db.MedicineDatabase
import com.phoenix.pillreminder.model.AlarmTriggeredViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel
import com.phoenix.pillreminder.model.MedicinesViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmTriggeredActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmTriggeredBinding
    private var mediaPlayer: MediaPlayer? = null
    private val viewModel: AlarmTriggeredViewModel by viewModels()
    private lateinit var medicinesViewModel: MedicinesViewModel
    private lateinit var factory: MedicinesViewModelFactory
    private lateinit var dao: MedicineDao

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAlarmTriggeredBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        dao = MedicineDatabase.getInstance(this).medicineDao()
        factory = MedicinesViewModelFactory(dao)

        medicinesViewModel = ViewModelProvider(this, this.factory)[MedicinesViewModel::class.java]

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        binding.apply{
            val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
            }else{
                intent?.getParcelableExtra("ALARM_ITEM")
            }

            viewModel.apply{
                alarmItem?.apply{
                    tvAlarmMedicineName.text = medicineName
                    tvAlarmHourMedicine.text = checkDateFormat(alarmHour.toInt(), alarmMinute.toInt(), context = applicationContext)
                    tvAlarmQuantity.text = checkMedicineForm(medicineForm, medicineQuantity, context = applicationContext)

                    ivAlarmMedicineIcon.setImageResource(setMedicineImageView(medicineForm))

                    btnPause.setOnClickListener {
                        stopMediaPlayer()
                    }

                    btnTaken.setOnClickListener{
                        stopMediaPlayer()

                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.markMedicineAsTaken(alarmItem, medicinesViewModel)
                        }
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                    }

                    btnDismiss.setOnClickListener {
                        stopMediaPlayer()
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }

        }



    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun stopMediaPlayer(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
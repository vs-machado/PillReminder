package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityAlarmTriggeredBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmTriggeredViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmTriggeredActivity: AppCompatActivity() {

    @Inject
    lateinit var repository: MedicineRepository

    private lateinit var medicinesViewModel: MedicinesViewModel
    private lateinit var binding: ActivityAlarmTriggeredBinding
    //private var mediaPlayer: MediaPlayer? = null
    private val viewModel: AlarmTriggeredViewModel by viewModels()
    //private lateinit var factory: MedicinesViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAlarmTriggeredBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(binding.root)

        setupNotificationAndStatusBar()

        /*mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()*/
        medicinesViewModel = ViewModelProvider(this)[MedicinesViewModel::class.java]

        binding.apply{
            val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
            }else{
                intent?.getParcelableExtra("ALARM_ITEM")
            }
            Log.d("alarmItem", "alarmtriggeredactivity: $alarmItem")

            viewModel.apply{
                alarmItem?.apply{
                    tvAlarmMedicineName.text = medicineName
                    tvAlarmHourMedicine.text = checkDateFormat(alarmHour.toInt(), alarmMinute.toInt(), context = applicationContext)
                    tvAlarmQuantity.text = checkMedicineForm(medicineForm, doseUnit, medicineQuantity, context = applicationContext)

                    ivAlarmMedicineIcon.setImageResource(setMedicineImageView(medicineForm))

                    /*btnPause.setOnClickListener {
                        stopMediaPlayer()
                    }*/

                    btnTaken.setOnClickListener{
                        //stopMediaPlayer()

                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.markMedicineAsTaken(alarmItem, medicinesViewModel)
                        }
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    btnDismiss.setOnClickListener {
                        //stopMediaPlayer()
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }

        }



    }

    private fun setupNotificationAndStatusBar(){
        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            window.statusBarColor = resources.getColor(R.color.blue_gradient_start, null)
            window.navigationBarColor = resources.getColor(R.color.blue_gradient_end, null)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        } else {
            window.statusBarColor = resources.getColor(R.color.blue_gradient_start, null)
            window.navigationBarColor = resources.getColor(R.color.white, null)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        /*mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null*/
    }

    /*private fun stopMediaPlayer(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }*/

}
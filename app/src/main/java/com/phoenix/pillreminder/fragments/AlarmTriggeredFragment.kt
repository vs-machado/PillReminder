package com.phoenix.pillreminder.fragments

import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentAlarmTriggeredBinding


class AlarmTriggeredFragment : Fragment(), MediaPlayer.OnPreparedListener {
    private lateinit var binding: FragmentAlarmTriggeredBinding
    private var mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAlarmTriggeredBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply{
            btnPause.setOnClickListener {
                mediaPlayer.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }

        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mediaPlayer.start()
    }
}
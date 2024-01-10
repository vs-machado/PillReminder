package com.phoenix.pillreminder.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.databinding.FragmentHowManyPerDayBinding

class HowManyPerDayFragment : Fragment() {
    private lateinit var binding: FragmentHowManyPerDayBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHowManyPerDayBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.apply{
            toolbarHowMany.setupWithNavController(navController, appBarConfiguration)
            setNumberPicker()
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setNumberPicker(){
        binding.apply{
            npHowOften.minValue = 0
            npHowOften.maxValue = 9
            npHowOften.textSize = 100F

            val pickerVals = intArrayOf(1,2,3,4,5,6,7,8,9,10)
            val stringPickerVals = pickerVals.map{it.toString()}.toTypedArray()

            npHowOften.displayedValues = stringPickerVals
        }
    }
}
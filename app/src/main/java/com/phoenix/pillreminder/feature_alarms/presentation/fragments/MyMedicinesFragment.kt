package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMyMedicinesBinding

class MyMedicinesFragment : Fragment() {
    private lateinit var binding: FragmentMyMedicinesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyMedicinesBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar(){
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbarHome.setupWithNavController(navController, appBarConfiguration)
        binding.toolbarHome.title = "Pill Reminder"
        binding.toolbarHome.setTitleTextColor(Color.WHITE)
        binding.toolbarHome.setNavigationIcon(R.drawable.baseline_menu_24)

        binding.toolbarHome.setNavigationOnClickListener {
            binding.dlMyMedicines.open()
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.home_item -> {
                    true
                }
                R.id.mymedicines_item -> {
                    menuItem.isChecked = true
                    findNavController().navigate(R.id.action_homeFragment_to_myMedicinesFragment)
                    true
                }
            }

            menuItem.isChecked = true
            binding.dlMyMedicines.close()
            true
        }
    }

}
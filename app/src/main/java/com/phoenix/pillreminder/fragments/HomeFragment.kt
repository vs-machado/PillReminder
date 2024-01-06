package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.apply {
            toolbarHome.setupWithNavController(navController, appBarConfiguration)


            rvMedicinesList.layoutManager = LinearLayoutManager(activity)
            rvMedicinesList.adapter = RvMedicinesListAdapter()

            fabAddMedicine.setOnClickListener {
                it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
            }
        }
    }
}
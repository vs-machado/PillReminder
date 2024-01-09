package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.databinding.FragmentFrequencyBinding
import com.phoenix.pillreminder.model.FrequencyViewModel

class FrequencyFragment : Fragment() {
    private lateinit var binding:FragmentFrequencyBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFrequencyBinding.inflate(layoutInflater)

        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val viewModel: FrequencyViewModel by viewModels()

        binding.apply{
            toolbarFrequency.setupWithNavController(navController, appBarConfiguration)

            // Med forms list. User must select the desired type of med
            val list: MutableList<String> = mutableListOf("Every day", "Every other day", "Specific days of the week", "On a recurring cycle", "Every X days",
                "Every X weeks", "Every X months")
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvFrequency.adapter = arrayAdapter

            // Check the user selected option and navigate to the next fragment
            lvFrequency.setOnItemClickListener { _, it, position, _ ->
                viewModel.checkSelectedOption(position)
                // Change destination fragment
                // it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
            }

        }

    }
}
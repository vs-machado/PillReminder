package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phoenix.pillreminder.databinding.FragmentMedicineFormBinding

class MedicineFormFragment : Fragment() {
    private lateinit var binding: FragmentMedicineFormBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMedicineFormBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply{
            // Med forms list. User must select the desired type of med
            val list: MutableList<String> = mutableListOf("Pill", "Injection", "Liquid", "Drops", "Inhaler",
                "Powder", "Other")
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvMedForm.adapter = arrayAdapter

            // Check the user selected option and navigate to the next fragment
            lvMedForm.setOnItemClickListener { _, _, position, _ ->
                checkSelectedOption(position)
            }


            // Go back to addMedicinesFragment
            ivBackToAddMedicines.setOnClickListener {
                findNavController().navigateUp()
            }
        }

    }

    private fun checkSelectedOption(position: Int){
        when (position){
            0 -> {
                // Should pass the user option to the database in the future

            }

            1 -> {
                // Should pass the user option to the database in the future
            }

            2 -> {
                // Should pass the user option to the database in the future
            }

            3 -> {
                // Should pass the user option to the database in the future
            }

            4 -> {
                // Should pass the user option to the database in the future
            }

            5 -> {
                // Should pass the user option to the database in the future
            }

            6 -> {
                // Should pass the user option to the database in the future
            }
        }
    }
}
package com.phoenix.pillreminder.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMedicineFormBinding

class MedicineFormFragment : Fragment() {
    private lateinit var binding: FragmentMedicineFormBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMedicineFormBinding.inflate(layoutInflater)

        binding.apply{
            // Med Forms. User must select the desired type of med
            val list: MutableList<String> = mutableListOf("Pill", "Injection", "Liquid", "Drops", "Inhaler",
                "Powder", "Other")
            // Sets the listView
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvMedForm.adapter = arrayAdapter

            // Go back to addMedicinesFragment
            ivBackToAddMedicines.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }
}
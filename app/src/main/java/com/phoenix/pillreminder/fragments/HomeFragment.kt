package com.phoenix.pillreminder.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
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

        binding.apply {
            rvMedicinesList.layoutManager = LinearLayoutManager(activity)
            rvMedicinesList.adapter = RvMedicinesListAdapter()

            fabAddMedicine.setOnClickListener {
                it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
            }
        }
        // Inflate the layout for this fragment
        return binding.root
    }
}
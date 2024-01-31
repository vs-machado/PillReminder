package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.MainActivity
import com.phoenix.pillreminder.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.databinding.FragmentHomeBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter
    private lateinit var medicinesViewModel: MedicinesViewModel
    private val sharedViewModel: AlarmSettingsSharedViewModel by viewModels()


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


        medicinesViewModel = ViewModelProvider(requireActivity(), (requireActivity() as MainActivity).factory)[MedicinesViewModel::class.java]


        binding.apply {
            toolbarHome.setupWithNavController(navController, appBarConfiguration)

            initRecyclerView()

            fabAddMedicine.setOnClickListener {
                it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
            }
        }
    }

    private fun initRecyclerView(){
        binding.rvMedicinesList.layoutManager = LinearLayoutManager(activity)
        adapter = RvMedicinesListAdapter()
        binding.rvMedicinesList.adapter = adapter

        displayMedicinesList()
    }

    private fun displayMedicinesList(){
        medicinesViewModel.medicines.observe(viewLifecycleOwner) {
            adapter.setList(it)
        }
    }
}
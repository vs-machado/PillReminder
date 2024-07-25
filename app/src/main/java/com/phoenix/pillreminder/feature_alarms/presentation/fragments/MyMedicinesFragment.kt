package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMyMedicinesBinding
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.MedicinesDataAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MyMedicinesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyMedicinesFragment : Fragment() {
    private lateinit var binding: FragmentMyMedicinesBinding
    private lateinit var adapter: MedicinesDataAdapter
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val myMedicinesViewModel: MyMedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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

        initRecyclerView()

        binding.searchView.clearFocus()
        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                displayMedicinesList(newText)
                return true
            }
        })


    }

    private fun initRecyclerView(){
        binding.rvMedicinesData.layoutManager = LinearLayoutManager(activity)
        adapter = MedicinesDataAdapter(requireContext()) {medicine ->
            val action = MyMedicinesFragmentDirections.actionMyMedicinesFragmentToMedicineDetailsFragment(medicine)
            findNavController().navigate(action)
        }
        binding.rvMedicinesData.adapter = adapter
        displayMedicinesList(null)
    }

    private fun displayMedicinesList(filter: String?){
        viewLifecycleOwner.lifecycleScope.launch {
            val medicines = withContext(Dispatchers.IO) {
                medicinesViewModel.getAllDistinctMedicines()
            }
            adapter.setAlarmsAndList(medicines, filter)
        }
    }


}
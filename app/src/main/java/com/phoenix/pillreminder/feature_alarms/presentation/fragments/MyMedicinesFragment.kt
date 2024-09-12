package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMyMedicinesBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.MedicinesDataAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MyMedicinesFragment : Fragment() {
    private lateinit var binding: FragmentMyMedicinesBinding
    private lateinit var adapter: MedicinesDataAdapter
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val filterOptions = mutableSetOf<Int>()
    private lateinit var filterString: String
    private lateinit var medicines: List<Medicine>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyMedicinesBinding.inflate(layoutInflater)

        requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine).visibility = View.GONE

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
                displayMedicinesList(newText, filterOptions)
                return true
            }
        })

        binding.ibFilter?.setOnClickListener { showFilterMenu() }
    }

    private fun initRecyclerView(){
        binding.rvMedicinesData.layoutManager = LinearLayoutManager(activity)
        adapter = MedicinesDataAdapter(requireContext()) {medicine ->
            val action = MyMedicinesFragmentDirections.actionMyMedicinesFragmentToMedicineDetailsFragment(medicine)
            findNavController().navigate(action)
        }
        binding.rvMedicinesData.adapter = adapter
        displayMedicinesList(null, filterOptions)
    }

    private fun displayMedicinesList(filter: String?, filterOptions: MutableSet<Int>){
        viewLifecycleOwner.lifecycleScope.launch {
            medicines = withContext(Dispatchers.IO) {
                medicinesViewModel.getLastAlarmFromAllDistinctMedicines()
            }
            filterString = filter ?: ""
            adapter.setAlarmsAndList(medicines, filter, filterOptions)
        }
    }

    private fun showFilterMenu(){
        val popup = binding.ibFilter?.let { PopupMenu(requireContext(), it) }
        popup?.menuInflater?.inflate(R.menu.my_medicines_filter_menu, popup.menu)

        // Set the initial checked state of menu items
        for (i in 0 until popup?.menu?.size()!!) {
            val item = popup.menu.getItem(i)
            item.isChecked = filterOptions.contains(item.itemId)
        }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            item.isChecked = !item.isChecked
            if (item.isChecked) {
                filterOptions.add(item.itemId)
            } else {
                filterOptions.remove(item.itemId)
            }
            applyFilters()
            true
        }

        popup.show()
    }

    private fun applyFilters(){
        adapter.setAlarmsAndList(medicines, filterString, filterOptions)
    }
}
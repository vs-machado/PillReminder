package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMedicineFormBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel

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

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

        binding.apply{
            toolbarMedicineForm.setupWithNavController(navController, appBarConfiguration)

            // Med forms list. User must select the desired type of med
            val list: MutableList<String> = mutableListOf("Pill", "Injection", "Liquid", "Drops", "Inhaler",
            "Pomade")
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvMedForm.adapter = arrayAdapter

            // Check the user selected option and navigate to the next fragment
            lvMedForm.setOnItemClickListener { _, it, position, _ ->
                sharedViewModel.saveMedicineForm(position)
                it.findNavController().navigate(R.id.action_medicineFormFragment_to_quantityAndStrengthFragment)
            }

        }

    }
}
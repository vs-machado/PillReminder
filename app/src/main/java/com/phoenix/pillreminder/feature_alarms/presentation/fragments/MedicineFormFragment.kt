package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMedicineFormBinding
import com.phoenix.pillreminder.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class MedicineFormFragment : Fragment() {
    private lateinit var binding: FragmentMedicineFormBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicineFormBinding.inflate(layoutInflater)

        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.colorPrimary,
            R.color.white,
            R.color.grayed_blue,
            R.color.dark_gray,
            isAppearanceLightStatusBar = false,
            isAppearanceLightNavigationBar = true,
            isAppearanceLightStatusBarNightMode = false,
            isAppearanceLightNavigationBarNightMode = false
        )

        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE
        requireActivity().findViewById<View>(R.id.divider).visibility = View.GONE
        requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine).visibility = View.GONE


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
            val list: MutableList<String> = mutableListOf(
                getString(R.string.pill),
                getString(R.string.injection), getString(R.string.liquid),
                getString(R.string.Drops), getString(R.string.inhaler),
                getString(R.string.pomade))
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvMedForm.adapter = arrayAdapter

            // Check the user selected option and navigate to the next fragment
            lvMedForm.setOnItemClickListener { _, it, position, _ ->
                sharedViewModel.saveMedicineForm(position)

                if(sharedViewModel.getMedicineForm() == "pomade") {
                    sharedViewModel.setMedicineQuantity(1F) // Pomade don't need to specify quantity
                    sharedViewModel.setDoseUnit("application")
                    it.findNavController().navigate(R.id.action_medicineFormFragment_to_frequencyFragment)
                    return@setOnItemClickListener
                }

                it.findNavController().navigate(R.id.action_medicineFormFragment_to_quantityAndStrengthFragment)
            }

        }

    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}
package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.phoenix.pillreminder.databinding.FragmentQuantityAndStrengthBinding
import com.phoenix.pillreminder.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class QuantityAndStrengthFragment : Fragment() {
    private lateinit var binding: FragmentQuantityAndStrengthBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuantityAndStrengthBinding.inflate(layoutInflater)

        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.colorPrimary,
            R.color.white,
            R.color.dark_gray,
            R.color.dark_gray,
            isAppearanceLightStatusBar = false,
            isAppearanceLightNavigationBar = true,
            isAppearanceLightStatusBarNightMode = false,
            isAppearanceLightNavigationBarNightMode = false
        )

        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE
        requireActivity().findViewById<View>(R.id.divider).visibility = View.GONE
        requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine).visibility = View.GONE

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
        val medicineForm = sharedViewModel.getMedicineForm()
        var selectedValue: String

        binding.apply{
            toolbar.setupWithNavController(navController, appBarConfiguration)

            sharedViewModel.medicineForm.observe(viewLifecycleOwner) {
                setEditTextMedicineForm(it)
            }

            etQuantity.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(quantity: CharSequence?, start: Int, before: Int, count: Int) {
                    val inputIsFilled = quantity?.isNotBlank() ?: false
                    fabNext.visibility = if(inputIsFilled) View.VISIBLE else View.INVISIBLE
                }

                override fun afterTextChanged(s: Editable?) {
                    val count = s.toString().toIntOrNull() ?: 0

                    when(medicineForm){
                        "pill" -> binding.tvForm.text = resources.getQuantityString(R.plurals.pills, count)
                        "drop" -> binding.tvForm.text = resources.getQuantityString(R.plurals.drops, count)
                        "inhaler" -> binding.tvForm.text = resources.getQuantityString(R.plurals.puffs, count)
                    }
                }
            })

            // Number picker is visible, allowing user to select the units of measurement
            if(medicineForm == "injection" || medicineForm == "inhaler"){
                npQuantity.visibility = View.VISIBLE
                tvForm.visibility = View.INVISIBLE

                var stringArray = emptyArray<String>()
                selectedValue = ""

                when(medicineForm){
                    "injection" -> {
                        stringArray += arrayOf(
                            requireContext().getString(R.string.mL),
                            requireContext().getString(R.string.syringe)
                        )
                        // Initialize selected value with first array element
                        selectedValue = "mL"
                    }
                    "inhaler" -> {
                        stringArray += arrayOf(
                            requireContext().getString(R.string.puff),
                            requireContext().getString(R.string.mL),
                            requireContext().getString(R.string.mg)
                        )
                        // Initialize selected value with first array element
                        selectedValue = "puff"
                    }
                }

                npQuantity.minValue = 0
                npQuantity.maxValue = stringArray.size - 1
                npQuantity.displayedValues = stringArray

                npQuantity.setOnValueChangedListener { _, _, newVal ->
                    selectedValue = when(stringArray[newVal]) {
                        requireContext().getString(R.string.mL) -> "mL"
                        requireContext().getString(R.string.syringe) -> "syringe"
                        requireContext().getString(R.string.puff) -> "puff"
                        requireContext().getString(R.string.mg) -> "mg"
                        else -> throw IllegalArgumentException("Invalid medicine form")
                    }
                }

            } else {
                // Number picker is invisible, user don't need to change the medication unit
                npQuantity.visibility = View.INVISIBLE
                tvForm.visibility = View.VISIBLE

                selectedValue = when(medicineForm){
                    "pill" -> "pill"
                    "drop" -> "drop"
                    "pomade" -> "application"
                    "liquid" -> "mL"
                    else -> throw IllegalArgumentException("Invalid medicine form")
                }
            }

            fabNext.setOnClickListener {
                sharedViewModel.setMedicineQuantity(etQuantity.text.toString().toFloat())
                sharedViewModel.setDoseUnit(selectedValue)
                it.findNavController().navigate(R.id.action_quantityAndStrengthFragment_to_frequencyFragment)
            }


        }
    }

    private fun setEditTextMedicineForm(medicineForm: String?){
        when(medicineForm){
            "pill" -> {
                binding.tvForm.text = getString(R.string.pills)
            }
            "drop" -> {
                binding.tvForm.text = getString(R.string.drops)
            }
            "pomade" -> {
                binding.tvForm.text = getString(R.string.grams)
            }
            "injection" ->{
                binding.tvForm.text = getString(R.string.mls)
            }
            "liquid" -> {
                binding.tvForm.text = getString(R.string.mls)
            }
            "inhaler" -> {
                binding.tvForm.text = getString(R.string.mgs)
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
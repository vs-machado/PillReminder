package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentQuantityAndStrengthBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel

class QuantityAndStrengthFragment : Fragment() {
    private lateinit var binding: FragmentQuantityAndStrengthBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuantityAndStrengthBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

        binding.apply{
            toolbar.setupWithNavController(navController, appBarConfiguration)

            sharedViewModel.medicineForm.observe(viewLifecycleOwner) {
                setEditTextMedicineForm(it)
            }

            etQuantity.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(quantity: CharSequence?, start: Int, before: Int, count: Int) {
                    val inputIsFilled = quantity?.isNotBlank() ?: false
                    val inputIsEmpty = !inputIsFilled

                    setFabVisibility(inputIsEmpty, fabNext)
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            fabNext.setOnClickListener {
                sharedViewModel.setMedicineQuantity(etQuantity.text.toString().toFloat())
                it.findNavController().navigate(R.id.action_quantityAndStrengthFragment_to_frequencyFragment)
            }


        }
    }

    private fun setFabVisibility(inputIsEmpty: Boolean, fabNext: FloatingActionButton){
        if (inputIsEmpty){
            fabNext.visibility = View.INVISIBLE
            return
        }
        fabNext.visibility = View.VISIBLE
    }

    private fun setEditTextMedicineForm(medicineForm: String?){
        when(medicineForm){
            "pill" -> {
                binding.tvForm.text = "pill(s)"
            }
            "drop" -> {
                binding.tvForm.text = "drop(s)"
            }
            "pomade" -> {
                binding.tvForm.text = "gram(s)"
            }
            "injection" ->{
                binding.tvForm.text = "mL(s)"
            }
            "liquid" -> {
                binding.tvForm.text = "mL(s)"
            }
            "inhaler" -> {
                binding.tvForm.text = "mg(s)"
            }
        }
    }

}
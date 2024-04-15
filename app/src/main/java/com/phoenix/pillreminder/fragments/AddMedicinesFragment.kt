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
import com.google.android.material.textfield.TextInputLayout
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentAddMedicinesBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel

class AddMedicinesFragment : Fragment() {
    private lateinit var binding: FragmentAddMedicinesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddMedicinesBinding.inflate(layoutInflater)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
        val medicineName = binding.tietMedicineName.text

        binding.apply {
            toolbarAddMedicines.setupWithNavController(navController, appBarConfiguration)

            tietMedicineName.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(medicineName: CharSequence?, start: Int, before: Int, count: Int) {
                    /*Checks if the medicine name is filled. If so, displays the FAB to navigate
                    to the next fragment*/
                    val inputIsFilled = medicineName?.isNotBlank() ?: false
                    val inputIsEmpty = !inputIsFilled

                    setFabVisibility(inputIsEmpty, fabNext, tilMedicineName)
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            fabNext.setOnClickListener {
                // Save medicine name input by user
                sharedViewModel.setMedicineName(medicineName.toString())
                it.findNavController().navigate(R.id.action_addMedicinesFragment_to_medicineFormFragment)
            }

        }

    }

    /*Checks if the medicine name is filled. If so, displays the FAB to navigate
    to the next fragment*/
    fun setFabVisibility(inputIsEmpty: Boolean, fabNext: FloatingActionButton, tilMedicineName: TextInputLayout){
        if (inputIsEmpty){
            fabNext.visibility = View.INVISIBLE
            tilMedicineName.helperText = getString(R.string.required)
            return
        }
        fabNext.visibility = View.VISIBLE
        tilMedicineName.helperText = ""
    }
}
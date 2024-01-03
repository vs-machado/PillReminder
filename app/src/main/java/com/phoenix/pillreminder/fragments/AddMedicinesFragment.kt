package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.databinding.FragmentAddMedicinesBinding

class AddMedicinesFragment : Fragment() {
    private lateinit var binding: FragmentAddMedicinesBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddMedicinesBinding.inflate(layoutInflater)

        binding.apply{
            tietMedicineName.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(medicineName: CharSequence?, start: Int, before: Int, count: Int) {
                    /*Checks if the medicine name is filled. If so, displays the FAB to navigate
                    to the next fragment*/
                    val inputIsFilled = medicineName?.isNotBlank() ?: false
                    val inputIsEmpty = !inputIsFilled

                    setFabVisibility(inputIsEmpty, fabNext)
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        TODO("Make the back toolbar button clickable, write the navigation between fragments using FAB, check deprecated onCLick reason on XML")

        // Inflate the layout for this fragment
        return binding.root
    }

    fun setFabVisibility(inputIsEmpty: Boolean, fabNext: FloatingActionButton){
        if (inputIsEmpty){
            fabNext.visibility = View.INVISIBLE
            return
        }
        fabNext.visibility = View.VISIBLE
    }
}
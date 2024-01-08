package com.phoenix.pillreminder.model

import android.view.View
import androidx.lifecycle.ViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AddMedicinesViewModel : ViewModel() {

    /*Checks if the medicine name is filled. If so, displays the FAB to navigate
    to the next fragment*/
     fun setFabVisibility(inputIsEmpty: Boolean, fabNext: FloatingActionButton){
        if (inputIsEmpty){
            fabNext.visibility = View.INVISIBLE
            return
        }
        fabNext.visibility = View.VISIBLE
    }

}
package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.phoenix.pillreminder.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
    private lateinit var binding: FragmentHelpBinding
    private var rotationAngle = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelpBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableLayoutTransitions()

        binding.apply{
            cvQuestion1.setOnClickListener {
                toggleAnswerVisibility(tvAnswer1)
                rotateIcon(ibQuestion1)
            }
            ibQuestion1.setOnClickListener {
                toggleAnswerVisibility(tvAnswer1)
                rotateIcon(ibQuestion1)
            }

            cvQuestion2.setOnClickListener {
                toggleAnswerVisibility(tvAnswer2)
                rotateIcon(ibQuestion2)
            }
            ibQuestion2.setOnClickListener {
                toggleAnswerVisibility(tvAnswer2)
                rotateIcon(ibQuestion2)
            }

            cvQuestion3.setOnClickListener {
                toggleAnswerVisibility(tvAnswer3)
                rotateIcon(ibQuestion3)
            }
            ibQuestion3.setOnClickListener {
                toggleAnswerVisibility(tvAnswer3)
                rotateIcon(ibQuestion3)
            }

            cvQuestion4.setOnClickListener {
                toggleAnswerVisibility(tvAnswer4)
                rotateIcon(ibQuestion4)
            }
            ibQuestion4.setOnClickListener {
                toggleAnswerVisibility(tvAnswer4)
                rotateIcon(ibQuestion4)
            }
        }

    }

    private fun toggleAnswerVisibility(text: TextView){
        if (text.visibility == View.GONE) {
            text.visibility = View.VISIBLE
        } else {
            text.visibility = View.GONE
        }
    }

    private fun rotateIcon(imageButton: ImageButton){
        val anim = ObjectAnimator.ofFloat(imageButton, "rotation", rotationAngle + 180f)
        anim.duration = 300
        anim.start()
        rotationAngle += 180
        rotationAngle %= 360
    }

    private fun enableLayoutTransitions(){
        binding.apply{
            listOf(clQuestion1,clQuestion2,clQuestion3,clQuestion4).forEach{
                it.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            }
        }
    }

}
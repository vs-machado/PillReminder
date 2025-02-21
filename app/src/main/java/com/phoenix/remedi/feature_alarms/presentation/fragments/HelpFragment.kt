package com.phoenix.remedi.feature_alarms.presentation.fragments

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
    private lateinit var binding: FragmentHelpBinding
    private val rotationAngles = mutableMapOf<ImageButton, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelpBinding.inflate(layoutInflater)
        requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine).visibility = View.GONE
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableLayoutTransitions()

        binding.apply{
            setupQuestionListener(cvQuestion1, tvAnswer1, ibQuestion1)
            setupQuestionListener(cvQuestion2, tvAnswer2, ibQuestion2)
            setupQuestionListener(cvQuestion3, tvAnswer3, ibQuestion3)
            setupQuestionListener(cvQuestion4, tvAnswer4, ibQuestion4)
        }

    }

    private fun setupQuestionListener(cardView: CardView, textView: TextView, imageButton: ImageButton){
        val clickListener = View.OnClickListener {
            toggleAnswerVisibility(textView)
            rotateIcon(imageButton)
        }
        cardView.setOnClickListener(clickListener)
        imageButton.setOnClickListener(clickListener)
    }

    private fun toggleAnswerVisibility(text: TextView) {
        text.visibility = if (text.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    private fun rotateIcon(imageButton: ImageButton){
        val currentAngle = rotationAngles.getOrDefault(imageButton, 0)
        val newAngle = currentAngle + 180

        val anim = ObjectAnimator.ofFloat(imageButton, "rotation", newAngle.toFloat())
        anim.duration = 300
        anim.start()

        rotationAngles[imageButton] = newAngle % 360
    }

    private fun enableLayoutTransitions(){
        binding.apply{
            listOf(clQuestion1,clQuestion2,clQuestion3,clQuestion4).forEach{
                it.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            }
        }
    }

}
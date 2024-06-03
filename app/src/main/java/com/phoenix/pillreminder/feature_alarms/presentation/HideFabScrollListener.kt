package com.phoenix.pillreminder.feature_alarms.presentation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HideFabScrollListener(
    private val fab: View
): RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if(dy > 0 && fab.isShown){
            fab.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(object: AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator){
                        super.onAnimationEnd(animation)
                        fab.visibility = View.GONE
                    }
                })
        } else if (dy < 0 && !fab.isShown){
            fab.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(object: AnimatorListenerAdapter(){
                    override fun onAnimationStart(animation: Animator){
                        super.onAnimationStart(animation)
                        fab.visibility = View.VISIBLE
                    }
                })
        }
    }
}
package com.phoenix.pillreminder.feature_alarms.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.pillreminder.databinding.AdapterAlarmsHourListBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour

class AlarmsHourListAdapter(
    private val showTimePickerDialog: (Int, String) -> Unit
): RecyclerView.Adapter<AlarmsHourListAdapter.AlarmsHourViewHolder>(){

    private val alarmsList = ArrayList<AlarmHour>()

    fun setAlarms(newAlarms: List<AlarmHour>){

        val diffCallback = object: DiffUtil.Callback() {
            override fun getOldListSize(): Int = alarmsList.size

            override fun getNewListSize(): Int = newAlarms.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return alarmsList[oldItemPosition].alarmHour == newAlarms[newItemPosition].alarmHour
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return alarmsList[oldItemPosition] == newAlarms[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        alarmsList.clear()
        alarmsList.addAll(newAlarms)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateAlarm(position: Int, newAlarmHour: String){
        alarmsList[position].alarmHour = newAlarmHour
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmsHourViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val alarmsBinding = AdapterAlarmsHourListBinding.inflate(layoutInflater, parent, false)
        return AlarmsHourViewHolder(alarmsBinding)
    }

    override fun onBindViewHolder(holder: AlarmsHourViewHolder, position: Int) {
        val alarm = alarmsList[position]
        holder.bind(alarm, position, showTimePickerDialog)
    }

    override fun getItemCount(): Int {
        return alarmsList.size
    }

    inner class AlarmsHourViewHolder(private val alarmsBinding: AdapterAlarmsHourListBinding): RecyclerView.ViewHolder(alarmsBinding.root){
        fun bind(
            alarm: AlarmHour,
            position: Int,
            showTimePickerDialog: (Int, String) -> Unit
        ){
            alarmsBinding.tvAlarms.text = alarm.alarmHour

            alarmsBinding.clAlarmHour.setOnClickListener {
                showTimePickerDialog(position, alarm.alarmHour)
            }
        }
    }
}
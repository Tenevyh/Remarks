package com.project.android.remarks

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*
import java.util.Calendar.*

private const val ARG_TIME = "time"

class TimePickerFragment: DialogFragment() {

    interface Callbacks {
        fun onTimeSelected(time: Date)
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog{

        val time = arguments?.getSerializable(ARG_TIME) as Date
        val calendar = Calendar.getInstance()
        calendar.time = time
        val calendarYear = calendar.get(YEAR)
        val calendarMonth = calendar.get(MONTH)
        val calendarDay = calendar.get(DAY_OF_MONTH)

        val timeListener = TimePickerDialog.OnTimeSetListener{ _: TimePicker, hour: Int, minute: Int ->
            val resultTime = GregorianCalendar(calendarYear, calendarMonth, calendarDay,hour, minute).time
            targetFragment?.let { fragment ->
                (fragment as Callbacks).onTimeSelected(resultTime)
            }
        }



        return TimePickerDialog(
            requireContext(),
            timeListener,
            calendar.get(HOUR_OF_DAY),
            calendar.get(MINUTE),
            true
        )
    }

    companion object{
        fun newInstance(time: Date): TimePickerFragment {
            var args = Bundle().apply {
                putSerializable(ARG_TIME, time)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }
}
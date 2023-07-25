package com.project.android.remarks

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File


class FullScreen (val photofile: File) : DialogFragment() {

    private lateinit var fullPhoto: ImageView


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.full_screen, container, false)
        fullPhoto = view.findViewById(R.id.fullscreen)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val image = getScaledBitmap(photofile.path, requireActivity())
        fullPhoto.setImageBitmap(image)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        fullPhoto.setOnClickListener{
            onDestroyView()
        }
    }

}

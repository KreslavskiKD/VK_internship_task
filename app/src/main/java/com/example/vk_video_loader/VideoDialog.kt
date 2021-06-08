package com.example.vk_video_loader

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


interface VideoDialogReturnInterface {
    fun onDialogPositiveClick(name : String, description : String, potw : Boolean, repeat : Boolean, compression : Boolean)
    fun onDialogNegativeClick()
}

class VideoDialog(var videoInterface : VideoDialogReturnInterface) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.video_dialog, null)

            builder.setView(view)
                .setPositiveButton("OK"
                ) { dialog, id ->
                    val name = (view.findViewById<EditText>(R.id.video_name)).text.toString()
                    val desc = (view.findViewById<EditText>(R.id.video_description)).text.toString()
                    val potw = (view.findViewById<CheckBox>(R.id.video_wall)).isActivated
                    val rept = (view.findViewById<CheckBox>(R.id.video_repeat)).isActivated
                    val comp = (view.findViewById<CheckBox>(R.id.video_compression)).isActivated
                    videoInterface.onDialogPositiveClick(name, desc, potw, rept, comp)
                }
                .setNegativeButton("Cancel"
                ) { dialog, id ->
                    videoInterface.onDialogNegativeClick()
                    dialog.cancel()
                }
                .setTitle("Insert Video details")
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


}

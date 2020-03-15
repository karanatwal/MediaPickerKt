package com.kandy.mediapicker

import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            choosePic{
                imageView.setImageURI(it)
            }
        }
    }


    private fun choosePic(callBack: (Uri) -> Unit) {
        val array: ArrayList<MediaItemType> = ArrayList()
        array.add(MediaItemType(MediaItemType.ITEM_CAMERA))
        array.add(MediaItemType(MediaItemType.ITEM_GALLERY))
        val pickerDialog = MediaPicker.Builder(this)
            .setTitle("Choose an option below")
            .setDialogStyle(MediaPicker.DIALOG_MATERIAL)
            .setItems(array)
            .setTitleTextColor(Color.WHITE)
            .setListType(MediaPicker.TYPE_LIST)
            .create()
        pickerDialog.show()
        pickerDialog.setPickerCloseListener { _, uri ->
            callBack.invoke(uri)
        }
    }
}

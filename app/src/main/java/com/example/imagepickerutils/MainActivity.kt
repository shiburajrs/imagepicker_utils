package com.example.imagepickerutils

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.simpleimagepicker.ImagePickerHelper

class MainActivity : AppCompatActivity() {

    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imagePickerHelper = ImagePickerHelper.register(
            activity = this,
            onImagePicked = { uri ->
                if (uri != null) {
                    findViewById<ImageView>(R.id.imageView).setImageURI(uri)
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            },
            onPermissionDenied = {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        )

        findViewById<Button>(R.id.pickImageButton).setOnClickListener {
            imagePickerHelper.pickImage()
        }
    }
}


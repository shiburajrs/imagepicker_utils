package com.example.imagepickerutils

import android.os.Bundle
import android.util.Log
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
            this,
            requestCode = 1001,
            enableCompression = true,
            fileName = "user_profile",
            onImagePicked = { code, uri ->
                Log.d("ImagePicker", "✅ Image picked (code=$code): $uri")
                uri?.let { findViewById<ImageView>(R.id.imageView).setImageURI(it) }
            },
            onPermissionDenied = {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        )

        findViewById<Button>(R.id.pickGalleryButton).setOnClickListener { imagePickerHelper.pickFromGallery() }
        findViewById<Button>(R.id.pickCameraButton).setOnClickListener { imagePickerHelper.pickFromCamera() }
    }
}


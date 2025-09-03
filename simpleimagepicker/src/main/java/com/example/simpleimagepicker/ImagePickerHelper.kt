package com.example.simpleimagepicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Future-proof Image Picker helper (supports Android 15+ photo picker)
 */
class ImagePickerHelper(
    private val context: Context,
    private val galleryLauncher: ActivityResultLauncher<String>,
    private val legacyLauncher: ActivityResultLauncher<android.content.Intent>,
    private val permissionLauncher: ActivityResultLauncher<String>,
    private val onImagePicked: (Uri?) -> Unit,
    private val onPermissionDenied: (() -> Unit)? = null
) {

    fun pickImage() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ → Use modern photo picker (no permission needed)
                galleryLauncher.launch("image/*")
            }
            needsLegacyPermission() -> {
                // Older versions → request permission
                requestLegacyPermission()
            }
            else -> {
                openLegacyGallery()
            }
        }
    }

    private fun needsLegacyPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    private fun requestLegacyPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openLegacyGallery() {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).apply {
            type = "image/*"
        }
        legacyLauncher.launch(intent)
    }

    fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            openLegacyGallery()
        } else {
            onPermissionDenied?.invoke()
        }
    }

    companion object {
        fun register(
            activity: AppCompatActivity,
            onImagePicked: (Uri?) -> Unit,
            onPermissionDenied: (() -> Unit)? = null
        ): ImagePickerHelper {
            lateinit var helper: ImagePickerHelper

            val modernLauncher = activity.registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                onImagePicked(uri)
            }

            val legacyLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val uri = result.data?.data
                onImagePicked(uri)
            }

            val permissionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                helper.handlePermissionResult(isGranted)
            }

            helper = ImagePickerHelper(
                context = activity,
                galleryLauncher = modernLauncher,
                legacyLauncher = legacyLauncher,
                permissionLauncher = permissionLauncher,
                onImagePicked = onImagePicked,
                onPermissionDenied = onPermissionDenied
            )
            return helper
        }

        fun register(
            fragment: Fragment,
            onImagePicked: (Uri?) -> Unit,
            onPermissionDenied: (() -> Unit)? = null
        ): ImagePickerHelper {
            lateinit var helper: ImagePickerHelper

            val modernLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                onImagePicked(uri)
            }

            val legacyLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val uri = result.data?.data
                onImagePicked(uri)
            }

            val permissionLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                helper.handlePermissionResult(isGranted)
            }

            helper = ImagePickerHelper(
                context = fragment.requireContext(),
                galleryLauncher = modernLauncher,
                legacyLauncher = legacyLauncher,
                permissionLauncher = permissionLauncher,
                onImagePicked = onImagePicked,
                onPermissionDenied = onPermissionDenied
            )
            return helper
        }
    }
}

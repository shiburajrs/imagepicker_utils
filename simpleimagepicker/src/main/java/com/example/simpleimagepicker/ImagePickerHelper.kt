package com.example.simpleimagepicker

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

/**
 * 📸 ImagePickerHelper — Simplified camera & gallery image picker for all Android versions.
 *
 * ✅ Supports:
 * - Android 13+ Photo Picker (no permission required)
 * - Legacy gallery access (Android 12 and below)
 * - Camera capture using MediaStore
 * - Custom file names (defaults to currentTimeMillis)
 * - Optional compression
 * - RequestCode-based callback identification
 *
 * 📱 Usage:
 * val picker = ImagePickerHelper.register(this, 101, true, "profile_image") { code, uri -> ... }
 * picker.pickFromGallery()
 * picker.pickFromCamera()
 */
class ImagePickerHelper private constructor(
    private val context: Context,
    private val requestCode: Int,
    private val enableCompression: Boolean,
    private val fileName: String?,
    private val galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val cameraLauncher: ActivityResultLauncher<Uri>,
    private val legacyLauncher: ActivityResultLauncher<Intent>,
    private val permissionLauncher: ActivityResultLauncher<String>,
    private val onImagePicked: (requestCode: Int, imageUri: Uri?) -> Unit,
    private val onPermissionDenied: (() -> Unit)? = null
) {

    private var cameraImageUri: Uri? = null

    // region ======== PUBLIC PICKERS ========

    /** Pick image from gallery (modern or legacy) */
    fun pickFromGallery() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                galleryLauncher.launch(request)
            }
            needsLegacyPermission() -> requestLegacyPermission()
            else -> openLegacyGallery()
        }
    }

    /** Pick image from camera */
    fun pickFromCamera() {
        cameraImageUri = createTempImageUri(fileName)
        cameraImageUri?.let { uri ->
            cameraLauncher.launch(uri)
        } ?: Log.e("ImagePickerHelper", "Failed to create temp URI for camera capture")
    }

    // endregion

    // region ======== INTERNAL HELPERS ========

    private fun needsLegacyPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        } else false
    }

    private fun requestLegacyPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openLegacyGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        legacyLauncher.launch(intent)
    }

    fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) openLegacyGallery() else onPermissionDenied?.invoke()
    }

    /** Creates a temp file URI for camera capture */
    private fun createTempImageUri(customName: String?): Uri? {
        return try {
            val imageFileName = "${customName ?: System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            Log.e("ImagePickerHelper", "Failed to create temp URI: ${e.message}")
            null
        }
    }

    /** Optionally compress image and return new Uri */
    private fun handleImageResult(uri: Uri?) {
        if (uri == null) {
            onImagePicked(requestCode, null)
            return
        }

        val finalUri = if (enableCompression) {
            compressImage(uri, fileName ?: System.currentTimeMillis().toString())
        } else uri

        onImagePicked(requestCode, finalUri)
    }

    private fun compressImage(originalUri: Uri, newFileName: String): Uri {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, originalUri)
            val compressedFile = File(context.cacheDir, "$newFileName.jpg")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            Uri.fromFile(compressedFile)
        } catch (e: Exception) {
            Log.e("ImagePickerHelper", "Image compression failed: ${e.message}")
            originalUri
        }
    }

    // endregion

    companion object {

        /** Register helper for Activity */
        fun register(
            activity: AppCompatActivity,
            requestCode: Int,
            enableCompression: Boolean = false,
            fileName: String? = null,
            onImagePicked: (requestCode: Int, imageUri: Uri?) -> Unit,
            onPermissionDenied: (() -> Unit)? = null
        ): ImagePickerHelper {
            lateinit var helper: ImagePickerHelper

            val galleryLauncher = activity.registerForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                helper.handleImageResult(uri)
            }

            val cameraLauncher = activity.registerForActivityResult(
                ActivityResultContracts.TakePicture()
            ) { success ->
                helper.handleImageResult(if (success) helper.cameraImageUri else null)
            }

            val legacyLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                helper.handleImageResult(result.data?.data)
            }

            val permissionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                helper.handlePermissionResult(granted)
            }

            helper = ImagePickerHelper(
                context = activity,
                requestCode = requestCode,
                enableCompression = enableCompression,
                fileName = fileName,
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                legacyLauncher = legacyLauncher,
                permissionLauncher = permissionLauncher,
                onImagePicked = onImagePicked,
                onPermissionDenied = onPermissionDenied
            )

            return helper
        }

        /** Register helper for Fragment */
        fun register(
            fragment: Fragment,
            requestCode: Int,
            enableCompression: Boolean = false,
            fileName: String? = null,
            onImagePicked: (requestCode: Int, imageUri: Uri?) -> Unit,
            onPermissionDenied: (() -> Unit)? = null
        ): ImagePickerHelper {
            lateinit var helper: ImagePickerHelper

            val galleryLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                helper.handleImageResult(uri)
            }

            val cameraLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.TakePicture()
            ) { success ->
                helper.handleImageResult(if (success) helper.cameraImageUri else null)
            }

            val legacyLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                helper.handleImageResult(result.data?.data)
            }

            val permissionLauncher = fragment.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                helper.handlePermissionResult(granted)
            }

            helper = ImagePickerHelper(
                context = fragment.requireContext(),
                requestCode = requestCode,
                enableCompression = enableCompression,
                fileName = fileName,
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                legacyLauncher = legacyLauncher,
                permissionLauncher = permissionLauncher,
                onImagePicked = onImagePicked,
                onPermissionDenied = onPermissionDenied
            )

            return helper
        }
    }
}

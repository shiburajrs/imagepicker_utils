package com.shiburajrs.mediapicker


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            else -> {
                openLegacyGallery()
            }
        }
    }


    /** Pick image from camera */
    fun pickFromCamera() {
        cameraImageUri = createTempImageUri(context,fileName)
        cameraImageUri?.let { uri ->
            cameraLauncher.launch(uri)
        } ?: Log.e("ImagePickerHelper", "Failed to create temp URI for camera capture")
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
    private fun createTempImageUri(
        context: Context,
        fileName: String?
    ): Uri {
        val imageFileName = "${fileName ?: System.currentTimeMillis()}.jpg"
        val imageFile = File(context.cacheDir, imageFileName)

        return FileProvider.getUriForFile(
            context,
            ImagePickerProvider.authority(context),
            imageFile
        )
    }


    /** Optionally compress image and return new Uri */
    private fun handleImageResult(uri: Uri?) {
        if (uri == null) {
            onImagePicked(requestCode, null)
            return
        }

        val finalUri = copyToCache(uri)
        onImagePicked(requestCode, finalUri)
    }

    private fun copyToCache(uri: Uri): Uri {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        return FileProvider.getUriForFile(
            context,
            ImagePickerProvider.authority(context),
            file
        )
    }

    internal fun verifyFileProvider(context: Context) {
        val authority = ImagePickerProvider.authority(context)

        val provider = context.packageManager
            .resolveContentProvider(authority, 0)

        if (provider == null) {
            throw IllegalStateException(
                "FileProvider with authority \"$authority\" not found.\n" +
                        "Please declare FileProvider in your app manifest.\n" +
                        "Refer to imagepicker_utils setup guide."
            )
        }
    }




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

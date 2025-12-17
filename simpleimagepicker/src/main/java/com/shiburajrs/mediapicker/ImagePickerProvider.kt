package com.shiburajrs.mediapicker

import android.content.Context

internal object ImagePickerProvider {

    fun authority(context: Context): String {
        return "${context.packageName}.provider"
    }
}

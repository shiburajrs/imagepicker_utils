# 📸 ImagePickerHelper

A **future-proof image picker** for Android that supports **Camera and Gallery**,  
with built-in **compression**, **custom file naming**, **Android 15+ Photo Picker API**, and **automatic permission handling**.

---

## 🚀 Features

✅ Pick from **Camera** or **Gallery**  
✅ **Android 13+ Photo Picker** (no storage permission needed)  
✅ **Legacy support** for Android 12 and below  
✅ **Optional compression** (JPEG, ~80% quality)  
✅ **Custom file names** or auto-generated names  
✅ **Request code support** for multiple pickers  
✅ Works in both **Activity** and **Fragment**  
✅ Automatically handles **permissions**  
✅ Clean Kotlin implementation — no `onActivityResult` needed

---

## 🧩 Supported Android Versions

| Android Version | Picker Type Used |
|-----------------|------------------|
| Android 13+ (API 33+) | **Photo Picker API (`PickVisualMedia`)** |
| Android 12 and below | **Legacy Gallery Intent (`ACTION_PICK`)** |
| All Versions | **Camera Capture (`TakePicture`)** |

---

## ⚙️ Installation

### Step 1 — Add the file
Copy **`ImagePickerHelper.kt`** into your project, for example:
```
app/src/main/java/com/yourapp/utils/ImagePickerHelper.kt
```

### Step 2 — Add permissions to `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.CAMERA" />
```

> ⚠️ Don’t worry — Android 13+ users won’t be prompted unnecessarily.  
> The helper requests permissions **only when needed** on older versions.

---

## 💻 Usage

### 🔹 Register the Helper (in Activity or Fragment)

```kotlin
private lateinit var imagePicker: ImagePickerHelper

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    imagePicker = ImagePickerHelper.register(
        activity = this,             // or fragment = this
        requestCode = 101,           // To identify which picker
        enableCompression = true,    // Enable/disable compression
        fileName = "profile_pic",    // Optional custom file name
        onImagePicked = { code, uri ->
            Log.d("ImagePicker", "✅ Picked image (code=$code): $uri")
            imageView.setImageURI(uri)
        },
        onPermissionDenied = {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
        }
    )
}
```

---

### 🔹 Pick from Gallery

```kotlin
imagePicker.pickFromGallery()
```

### 🔹 Pick from Camera

```kotlin
imagePicker.pickFromCamera()
```

---

## 🧾 Callback Parameters

| Parameter | Type | Description |
|------------|------|-------------|
| `requestCode` | `Int` | Your custom identifier for this picker |
| `imageUri` | `Uri?` | Picked or captured image URI (may be `null` if cancelled) |

---

## 📁 File Handling

| Type | Storage Location |
|------|------------------|
| Gallery (modern) | Android system photo picker |
| Gallery (legacy) | MediaStore content provider |
| Camera | `Pictures/` folder (MediaStore) |
| Compressed Image | `context.cacheDir` (temporary file) |

---

## 🧱 Internal Features Summary

| Function | Description |
|-----------|-------------|
| `pickFromGallery()` | Opens Photo Picker (Android 13+) or legacy gallery |
| `pickFromCamera()` | Captures image and stores it in MediaStore |
| `compressImage()` | Compresses image to ~80% JPEG quality |
| `createTempImageUri()` | Prepares a URI for camera output |
| `handlePermissionResult()` | Handles permission grant/denial automatically |
| `register()` | Registers ActivityResult launchers and callbacks |

---

## 🧠 Example UI Integration

**activity_main.xml**
```xml
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="24dp">

    <Button
        android:id="@+id/btnGallery"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Pick from Gallery" />

    <Button
        android:id="@+id/btnCamera"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="Pick from Camera" />

    <ImageView
        android:id="@+id/imageView"
        android:layout_width="180dp"
        android:layout_height="180dp"
        android:layout_marginTop="24dp"
        android:scaleType="centerCrop"
        android:background="@android:color/darker_gray"/>
</LinearLayout>
```

**MainActivity.kt**
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var picker: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        picker = ImagePickerHelper.register(
            activity = this,
            requestCode = 1001,
            enableCompression = true,
            fileName = "user_profile",
            onImagePicked = { code, uri ->
                Log.d("ImagePicker", "Picked (code=$code): $uri")
                uri?.let { findViewById<ImageView>(R.id.imageView).setImageURI(it) }
            },
            onPermissionDenied = {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        )

        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            picker.pickFromGallery()
        }

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            picker.pickFromCamera()
        }
    }
}
```

---

## 🧠 Optional — Multiple Pickers Example

You can register multiple pickers with different request codes:
```kotlin
val profilePicker = ImagePickerHelper.register(this, 2001, onImagePicked = { code, uri ->
    if (code == 2001) profileImageView.setImageURI(uri)
})

val coverPicker = ImagePickerHelper.register(this, 2002, onImagePicked = { code, uri ->
    if (code == 2002) coverImageView.setImageURI(uri)
})
```

---

## ⚙️ Compression Behavior

- Compression is **optional** (`enableCompression = true`).
- Saved as JPEG (80% quality).
- Stored in app’s `cacheDir`, automatically cleaned up by system.

---

## 🔐 Permissions Behavior

| Android Version | Required Permission |
|-----------------|---------------------|
| Android 13+ | ❌ None |
| Android 10–12 | ✅ `READ_EXTERNAL_STORAGE` |
| Android ≤ 9 | ✅ `READ_EXTERNAL_STORAGE` |
| Camera | ✅ `CAMERA` |

---

## 🧠 Recommended Libraries for Loading Image
For smoother image display, use:
- [Glide](https://github.com/bumptech/glide)
- [Coil](https://coil-kt.github.io/coil/)
- [Picasso](https://square.github.io/picasso/)

---

## 📜 License

```
MIT License

Copyright (c) 2025 Shiburaj RS

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files...
```

---

## ✨ Author

Developed with ❤️ by **Shiburaj (https://github.com/shiburajrs)**  
Made for Android Developers who value **clean, version-safe APIs**.

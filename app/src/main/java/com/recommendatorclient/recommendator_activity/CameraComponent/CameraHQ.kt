package com.recommendatorclient.recommendator_activity.CameraComponent

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.recommendatorclient.recommendation_service.RecommendatorApiClient
import java.io.File

class CameraHQ(val activity: FragmentActivity, val apiClient: RecommendatorApiClient) {
    lateinit var photoFile: File
    private val FILE_NAME = "barcode_photo"

    fun checkCameraPermission() {
        Dexter.withContext(activity.applicationContext)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            ).withListener(
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if (report.areAllPermissionsGranted()) {
//                                onTakePhotoClick()
                            } else {
                                showRotationalDialogForPermission()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRotationalDialogForPermission()
                    }
                }
            ).onSameThread().check()
    }

    private fun showRotationalDialogForPermission() {
        AlertDialog.Builder(activity.applicationContext)
            .setMessage(
                "Похоже, что вы отключили разрешения необходимые для этой возможности." +
                        "Их можно включить в системных настройках для приложения!"
            ).setPositiveButton("Открыть настройки") { dialog, id ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Отмена") { dialog, id ->
                dialog.dismiss()
            }.show()
    }

    fun onTakePhotoClick(startForResult: ActivityResultLauncher<Intent>) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val storageDirectory = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        photoFile = File.createTempFile(FILE_NAME, ".jpg", storageDirectory)

        // This DOESN'T work for API >= 24 (starting 2016)
        // takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)
        Log.d("Track", photoFile.absolutePath)
        val fileProvider = FileProvider.getUriForFile(activity,
            "com.recommendatorclient.provider", photoFile)
        Log.d("Track", fileProvider.path.toString())
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
//         takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)

        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            startForResult.launch(takePictureIntent)
        } else {
            Toast.makeText(activity.applicationContext, "Не удалось запустить камеру", Toast.LENGTH_SHORT).show()
        }
    }

    fun getTakenPhoto(): Bitmap? {
        if (!this::photoFile.isInitialized) {
            return null
        }
        val bitmapImg = BitmapFactory.decodeFile(photoFile.absolutePath)
        return bitmapImg
    }

    fun getTakenPhotoResized(): Bitmap? {
        if (!this::photoFile.isInitialized) {
            return null
        }
        val bitmapImg = BitmapFactory.decodeFile(photoFile.absolutePath)

        val aspectRatio: Float = bitmapImg.width.toFloat() / bitmapImg.height.toFloat()

        val width = 1000
        val height = Math.round(width / aspectRatio)
        val resizedImg = Bitmap.createScaledBitmap(bitmapImg, width, height, false)
        return resizedImg
    }

    fun getFileProviderImg(): Bitmap? {
        if (!this::photoFile.isInitialized) {
            return null
        }
        val fileProvider = FileProvider.getUriForFile(activity.applicationContext,
            "com.recommendatorclient.provider", photoFile)

        val bitmapImg = BitmapFactory.decodeFile(fileProvider.path)
        return bitmapImg
    }
//
//    private fun disableButtons(_binding: FragmentSearchBinding?) {
//        _binding?.btnSearch?.isActivated = false
//        _binding?.btnPing?.isActivated = false
//        _binding?.btnPingGoogle?.isActivated = false
//    }
//
//    private fun enableButtons(_binding: FragmentSearchBinding?) {
//        _binding?.btnSearch?.isActivated = true
//        _binding?.btnPing?.isActivated = true
//        _binding?.btnPingGoogle?.isActivated = true
//    }
}

// https://www.youtube.com/watch?v=DPHkhamDoyc&ab_channel=RahulPandey

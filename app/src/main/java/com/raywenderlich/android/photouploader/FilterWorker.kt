package com.skanderjabouzi.workmanager

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Images.Media.getBitmap
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

private const val LOG_TAG = "### FilterWorker"
const val KEY_IMAGE_URI = "IMAGE_URI"
const val KEY_IMAGE_INDEX = "IMAGE_INDEX"

private const val IMAGE_PATH_PREFIX = "IMAGE_PATH_"

class FilterWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result = try {
        Thread.sleep(3000)
        Log.d(LOG_TAG, "Applying filter to image")

        val imageUriString = inputData.getString(KEY_IMAGE_URI)
        val imageIndex = inputData.getInt(KEY_IMAGE_INDEX, 0)
        val bitmap = getBitmap(applicationContext.contentResolver, Uri.parse(imageUriString))
        val filtredBitmap = ImageUtils.applySepiaFilter(bitmap)
        val filtredImageUri = ImageUtils.writeBitmapToFile(applicationContext, filtredBitmap)

        val outputData = workDataOf(IMAGE_PATH_PREFIX + imageIndex to filtredImageUri.toString())
        Log.d(LOG_TAG, "Success!")
        Result.success(outputData)
    } catch(e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: " + e.message, e)
        Result.failure()
    }
}
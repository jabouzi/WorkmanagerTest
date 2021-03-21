package com.skanderjabouzi.workmanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.*
import kotlinx.android.synthetic.main.activity_main.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val UNIQUE_WORK_NAME = "UNIQUE_WORK_NAME"
        private const val WORK_TAG = "WORK_TAG"
        const val TAG_OUTPUT = "OUTPUT"
        const val TAG_PROGRESS = "TAG_PROGRESS"
        const val PROGRESS = "PROGRESS"
    }
    private val workManager = WorkManager.getInstance(application)
    internal val outputWorkInfos: LiveData<List<WorkInfo>>
    internal val progressWorkInfoItems: LiveData<List<WorkInfo>>

    init {
        // This transformation makes sure that whenever the current work Id changes the WorkInfo
        // the UI is listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
        progressWorkInfoItems = workManager.getWorkInfosByTagLiveData(TAG_PROGRESS)
    }

    internal fun cancelWorker() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun startFooWorker() {
        val fooWorker = OneTimeWorkRequest.Builder(FooWorker::class.java).addTag(TAG_PROGRESS).build()
        workManager.beginWith(fooWorker)
                .enqueue()

//        workManager.getWorkInfosForUniqueWorkLiveData(TAG_PROGRESS)
//                .observe(getApplication(), { workInfos ->
//                    if (workInfos.size > 0) {
//                        val info = workInfos[0]
//                        val progress = info.progress.getInt("progress", -1)
//                        Log.e("### MainViewModel", "progress: $progress")
//                        progressBar2.progress = progress
//                    }
//                })
    }

    fun startWorker(data: Intent) {
        val applySepiaFilter = buildSepiaFilterRequests(data)
        val zipFiles = OneTimeWorkRequest.Builder(CompressWorker::class.java).build()
        val uploadZip = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(
                        NetworkType.CONNECTED).build())
                .addTag(TAG_OUTPUT)
                .build()
        val cleanFiles = OneTimeWorkRequest.Builder(CleanFilesWorker::class.java).build()
        workManager.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, cleanFiles)
                .then(applySepiaFilter)
                .then(zipFiles)
                .then(uploadZip)
                .enqueue()
    }

    private fun buildSepiaFilterRequests(intent: Intent): List<OneTimeWorkRequest> {
        val filterRequests = mutableListOf<OneTimeWorkRequest>()
        Log.e("### MainViewModel", "intent.clipData: ${intent.clipData}")
        Log.e("### MainViewModel", "intent.data: ${intent.data}")
        intent.clipData?.run {
            for (i in 0 until itemCount) {
                val imageUri = getItemAt(i).uri

                val filterRequest = OneTimeWorkRequest.Builder(FilterWorker::class.java)
                        .setInputData(buildInputDataForFilter(imageUri, i))
                        .build()
                filterRequests.add(filterRequest)
            }
        }

        intent.data?.run {
            val filterWorkRequest = OneTimeWorkRequest.Builder(FilterWorker::class.java)
                    .setInputData(buildInputDataForFilter(this, 0))
                    .build()

            filterRequests.add(filterWorkRequest)
        }

        Log.e("### MainViewModel", "filterRequests $filterRequests")
        return filterRequests
    }

    private fun buildInputDataForFilter(imageUri: Uri?, index: Int): Data {
        val builder = Data.Builder()
        if (imageUri != null) {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
            builder.putInt(KEY_IMAGE_INDEX, index)
        }
        Log.e("### MainViewModel", "imageUri $imageUri")
        Log.e("### MainViewModel", "builder $builder")
        return builder.build()
    }

}
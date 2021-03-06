/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.skanderjabouzi.workmanager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.*
import com.skanderjabouzi.workmanager.MainViewModel.Companion.PROGRESS
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  companion object {
    private const val GALLERY_REQUEST_CODE = 300
    private const val PERMISSIONS_REQUEST_CODE = 301

    private const val MAX_NUMBER_REQUEST_PERMISSIONS = 2

    private const val IMAGE_TYPE = "image/*"
    private const val IMAGE_CHOOSER_TITLE = "Select Picture"


    private val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
  }

  private var permissionRequestCount = 0
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    initUi()
    requestPermissionsIfNecessary()
    observeWork()
  }

  private fun initUi() {
    uploadGroup.visibility = View.GONE
    pickPhotosButton.setOnClickListener {
      showPhotoPicker()
    }
    cancelButton.setOnClickListener {
      viewModel.cancelWorker()
    }
    testButton.setOnClickListener {
      viewModel.startFooWorker()
    }

    viewModel.outputWorkInfos.observe(this, observeWork())

    // Show work progress
    viewModel.progressWorkInfoItems.observe(this, observeProgress())

  }

  private fun showPhotoPicker() {
    val intent = Intent().apply {
      type = IMAGE_TYPE
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      action = Intent.ACTION_OPEN_DOCUMENT
    }

    startActivityForResult(Intent.createChooser(intent, IMAGE_CHOOSER_TITLE), GALLERY_REQUEST_CODE)
  }

  private fun requestPermissionsIfNecessary() {
    if (!hasRequiredPermissions()) {
      askForPermissions()
    }
  }

  private fun askForPermissions() {
    if (permissionRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
      permissionRequestCount += 1

      ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    } else {
      pickPhotosButton.isEnabled = false
    }
  }

  private fun hasRequiredPermissions(): Boolean {
    val permissionResults = PERMISSIONS.map { permission ->
      ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
    }

    return permissionResults.all { isGranted -> isGranted }
  }

  override fun onRequestPermissionsResult(
          code: Int,
          permissions: Array<String>,
          result: IntArray) {
    super.onRequestPermissionsResult(code, permissions, result)
    if (code == PERMISSIONS_REQUEST_CODE) {
      requestPermissionsIfNecessary()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (data != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
      Log.e("### MainActivity", "requestCode: $requestCode, resultCode: $resultCode, data: $data")
      viewModel.startWorker(data)
    }
  }

  private fun observeWork(): Observer<List<WorkInfo>> {

    return Observer { listOfWorkInfo ->
      if (listOfWorkInfo.isNullOrEmpty()) {
        return@Observer
      }
      val workInfo = listOfWorkInfo[0]

      if (workInfo.state.isFinished) {
        uploadGroup.visibility = View.GONE
      } else {
        uploadGroup.visibility = View.VISIBLE
      }
    }


//    uploadGroup.visibility = View.GONE
//    val statuses = WorkManager.getInstance(this).getWorkInfosByTagLiveData(WORK_TAG)
//    statuses.observe(this, Observer<List<WorkInfo>> { workStatusList ->
//      val currentWorkStatus = workStatusList?.getOrNull(0)
//      val isWorkActive = currentWorkStatus?.state?.isFinished == false
//
//      val uploadVisibility = if (isWorkActive) View.VISIBLE else View.GONE
//
//      uploadGroup.visibility = uploadVisibility
//    })
  }

  private fun observeProgress(): Observer<List<WorkInfo>> {
    return Observer { listOfWorkInfo ->
      if (listOfWorkInfo.isNullOrEmpty()) {
        return@Observer
      }

      listOfWorkInfo.forEach { workInfo ->
        if (WorkInfo.State.RUNNING == workInfo.state) {
          val progress = workInfo.progress.getInt(PROGRESS, 0)
          progressBar2.progress = progress
        }
      }

    }
  }

}

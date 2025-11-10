package com.canhub.cropper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.canhub.cropper.CropImage.CROP_IMAGE_ACTIVITY_RESULT_RETRY_CODE
import com.canhub.cropper.CropImageView.CropResult
import com.canhub.cropper.databinding.CropImageActivityBinding
import com.canhub.cropper.utils.DialogUtils
import com.canhub.cropper.utils.getUriForFile
import com.canhub.cropper.utils.isPermissionDeclared
import java.io.File

open class CropImageActivity :
  AppCompatActivity(),
  CropImageView.OnSetImageUriCompleteListener,
  CropImageView.OnCropImageCompleteListener {

  /** Persist URI image to crop URI if specific permissions are required. */
  private var cropImageUri: Uri? = null

  /** The options that were set for the crop image*/
  private lateinit var cropImageOptions: CropImageOptions

  /** The crop image view library widget used in the activity. */
  private var cropImageView: CropImageView? = null
  private lateinit var binding: CropImageActivityBinding
  private var latestTmpUri: Uri? = null
  private val pickImageGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (isLowResolutionImage(this, uri))
      showImageQualityLowDialog()
    else
      onPickImageResult(uri)
  }

  private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
    if (it) {
      onPickImageResult(latestTmpUri)
    } else {
      onPickImageResult(null)
    }
  }

  private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      takePicture()
    } else {
      showPermissionDeniedDialog()
    }
  }

  private val appSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
    takePicture()
  }

  private fun takePicture() {
    val isCameraPermissionDeclared = isPermissionDeclared(this, android.Manifest.permission.CAMERA)
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    if (!isCameraPermissionDeclared) {
      val tmpUri = getTmpFileUri()
      latestTmpUri = tmpUri
      takePicture.launch(tmpUri)
      return
    }

    if (hasCameraPermission) {
      getTmpFileUri().let { uri ->
        latestTmpUri = uri
        latestTmpUri?.let {
          takePicture.launch(it)
        }
      }
    } else {
      requestCameraPermission.launch(Manifest.permission.CAMERA)
    }
  }

  private fun showPermissionDeniedDialog() {
    AlertDialog.Builder(this)
      .setTitle(R.string.permission_required_text)
      .setMessage(R.string.camera_permission_text)
      .setCancelable(false)
      .setPositiveButton(R.string.go_to_setting) { _, _ ->
        openAppSettings()
      }
      .setNegativeButton(R.string.cancel) { _, _ ->
        onPickImageResult(null)
      }
      .show()
  }

  private fun showImageQualityLowDialog() {
    setActionButtonsVisibility(false)
    DialogUtils.showImageQualityLowDialog(this, ::setResultCancel)
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    binding = CropImageActivityBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setCropImageView(binding.cropImageView)
    val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
    cropImageUri = bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
    cropImageOptions =
      bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS) ?: CropImageOptions()

    if (savedInstanceState == null) {
      if (cropImageUri == null || cropImageUri == Uri.EMPTY) {
        when {
          cropImageOptions.showIntentChooser          -> showIntentChooser()
          cropImageOptions.imageSourceIncludeGallery &&
            cropImageOptions.imageSourceIncludeCamera ->
            showImageSourceDialog(::openSource)

          cropImageOptions.imageSourceIncludeGallery  ->
            pickImageGallery.launch("image/*")

          cropImageOptions.imageSourceIncludeCamera   ->
            openCamera()

          else                                        -> finish()
        }
      } else {
        cropImageView?.setImageUriAsync(cropImageUri)
      }
    } else {
      latestTmpUri = savedInstanceState.getString(BUNDLE_KEY_TMP_URI)?.toUri()
    }

    setCustomizations()

    onBackPressedDispatcher.addCallback {
      setResultCancel()
    }

    binding.btnRetry.setOnClickListener {
      setResultRetry()
    }

    binding.btnOk.setOnClickListener {
      cropImage()
    }
  }

  private fun setCustomizations() {
    cropImageOptions.activityBackgroundColor.let { activityBackgroundColor ->
      binding.root.setBackgroundColor(activityBackgroundColor)
    }
  }

  private fun showIntentChooser() {
    val ciIntentChooser = CropImageIntentChooser(
      activity = this,
      callback = object : CropImageIntentChooser.ResultCallback {
        override fun onSuccess(uri: Uri?) {
          onPickImageResult(uri)
        }

        override fun onCancelled() {
          setResultCancel()
        }
      },
    )
    cropImageOptions.let { options ->
      options.intentChooserTitle
        ?.takeIf { title ->
          title.isNotBlank()
        }
        ?.let { icTitle ->
          ciIntentChooser.setIntentChooserTitle(icTitle)
        }
      options.intentChooserPriorityList
        ?.takeIf { appPriorityList -> appPriorityList.isNotEmpty() }
        ?.let { appsList ->
          ciIntentChooser.setupPriorityAppsList(appsList)
        }
      val cameraUri: Uri? = if (options.imageSourceIncludeCamera) getTmpFileUri() else null
      ciIntentChooser.showChooserIntent(
        includeCamera = options.imageSourceIncludeCamera,
        includeGallery = options.imageSourceIncludeGallery,
        cameraImgUri = cameraUri,
      )
    }
  }

  private fun openSource(source: Source) {
    when (source) {
      Source.CAMERA  -> openCamera()
      Source.GALLERY -> pickImageGallery.launch("image/*")
    }
  }

  private fun openCamera() {
    takePicture()
  }

  private fun getTmpFileUri(): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
      createNewFile()
      deleteOnExit()
    }

    return getUriForFile(this, tmpFile)
  }

  /**
   * This method show the dialog for user source choice, it is an open function so can be overridden
   * and customised with the app layout if you need.
   */
  open fun showImageSourceDialog(openSource: (Source) -> Unit) {
    setActionButtonsVisibility(false)
    DialogUtils.showImageSourceDialog(
        this,
        onCameraSelected = {
            openSource(Source.CAMERA)
        },
        onGallerySelected = {
            openSource(Source.GALLERY)
        },
    )
  }

  private fun setActionButtonsVisibility(visible: Boolean) {
    binding.llActionButtons.isVisible = visible
  }

  public override fun onStart() {
    super.onStart()
    cropImageView?.setOnSetImageUriCompleteListener(this)
    cropImageView?.setOnCropImageCompleteListener(this)
  }

  public override fun onStop() {
    super.onStop()
    cropImageView?.setOnSetImageUriCompleteListener(null)
    cropImageView?.setOnCropImageCompleteListener(null)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(BUNDLE_KEY_TMP_URI, latestTmpUri.toString())
  }

  protected open fun onPickImageResult(resultUri: Uri?) {
    when (resultUri) {
      null -> setResultCancel()
      else -> {
        cropImageUri = resultUri
        cropImageView?.setImageUriAsync(cropImageUri)
      }
    }
    setActionButtonsVisibility(true)
  }

  override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
    if (error == null) {
      if (cropImageOptions.initialCropWindowRectangle != null) {
        cropImageView?.cropRect = cropImageOptions.initialCropWindowRectangle
      }

      if (cropImageOptions.initialRotation > 0) {
        cropImageView?.rotatedDegrees = cropImageOptions.initialRotation
      }

      if (cropImageOptions.skipEditing) {
        cropImage()
      }
    } else {
      setResult(null, error, 1)
    }
  }

  override fun onCropImageComplete(view: CropImageView, result: CropResult) {
    setResult(result.uriContent, result.error, result.sampleSize)
  }

  /**
   * Execute crop image and save the result tou output uri.
   */
  open fun cropImage() {
    if (cropImageOptions.noOutputImage) {
      setResult(null, null, 1)
    } else {
      cropImageView?.croppedImageAsync(
        saveCompressFormat = cropImageOptions.outputCompressFormat,
        saveCompressQuality = cropImageOptions.outputCompressQuality,
        reqWidth = cropImageOptions.outputRequestWidth,
        reqHeight = cropImageOptions.outputRequestHeight,
        options = cropImageOptions.outputRequestSizeOptions,
        customOutputUri = cropImageOptions.customOutputUri,
      )
    }
  }

  /**
   * When extending this activity, please set your own ImageCropView
   */
  open fun setCropImageView(cropImageView: CropImageView) {
    this.cropImageView = cropImageView
  }

  /**
   * Rotate the image in the crop image view.
   */
  open fun rotateImage(degrees: Int) {
    cropImageView?.rotateImage(degrees)
  }

  /**
   * Result with cropped image data or error if failed.
   */
  open fun setResult(uri: Uri?, error: Exception?, sampleSize: Int) {
    setResult(
      error?.let { CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE } ?: RESULT_OK,
      getResultIntent(uri, error, sampleSize),
    )
    finish()
  }

  /**
   * Cancel of cropping activity.
   */
  open fun setResultCancel() {
    setResult(RESULT_CANCELED)
    finish()
  }

  open fun setResultRetry() {
    setResult(CROP_IMAGE_ACTIVITY_RESULT_RETRY_CODE)
    finish()
  }

  /**
   * Get intent instance to be used for the result of this activity.
   */
  open fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
    val result = CropImage.ActivityResult(
      originalUri = cropImageView?.imageUri,
      uriContent = uri,
      error = error,
      cropPoints = cropImageView?.cropPoints,
      cropRect = cropImageView?.cropRect,
      rotation = cropImageView?.rotatedDegrees ?: 0,
      wholeImageRect = cropImageView?.wholeImageRect,
      sampleSize = sampleSize,
    )
    val intent = Intent()
    intent.extras?.let(intent::putExtras)
    intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
    return intent
  }

  /**
   * Update the color of a specific menu item to the given color.
   */
  open fun updateMenuItemIconColor(menu: Menu, itemId: Int, color: Int) {
    val menuItem = menu.findItem(itemId)
    if (menuItem != null) {
      val menuItemIcon = menuItem.icon
      if (menuItemIcon != null) {
        try {
          menuItemIcon.apply {
            mutate()
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
              color,
              BlendModeCompat.SRC_ATOP,
            )
          }
          menuItem.icon = menuItemIcon
        } catch (e: Exception) {
          Log.w("AIC", "Failed to update menu item color", e)
        }
      }
    }
  }

  private fun openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    appSettingsLauncher.launch(intent)
  }

  fun isLowResolutionImage(context: Context, imageUri: Uri?): Boolean {
    if (imageUri == null) return false
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

    context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
      BitmapFactory.decodeStream(inputStream, null, options)
    }

    val width = options.outWidth
    val height = options.outHeight

    return width < cropImageOptions.minCropWindowWidth && height < cropImageOptions.minCropWindowHeight
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

  }

  enum class Source { CAMERA, GALLERY }

  private companion object {

    const val BUNDLE_KEY_TMP_URI = "bundle_key_tmp_uri"
  }
}

package com.canhub.cropper.utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.graphics.drawable.toDrawable
import com.canhub.cropper.R

object DialogUtils {

  var dialog: Dialog? = null

  fun showImageSourceDialog(
    activity: Activity?,
    onCameraSelected : () -> Unit,
    onGallerySelected : () -> Unit
  ) {
    dialog?.dismissIfShowingSafely(activity)
    activity?.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
      dialog =
        Dialog(activity).apply {
          requestWindowFeature(Window.FEATURE_NO_TITLE)
          setContentView(R.layout.image_source_dialog)
          window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
          window?.setLayout(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
          )
          setCancelable(false)

          val actionCamera = findViewById<LinearLayout>(R.id.llCamera)
          val actionGallery = findViewById<LinearLayout>(R.id.llGallery)

          actionCamera?.setOnClickListener {
            dismiss()
            onCameraSelected.invoke()
          }

          actionGallery?.setOnClickListener {
            dismiss()
            onGallerySelected.invoke()
          }
          setOnDismissListener { dialog = null }
          show()
        }
    }
  }

  fun showImageQualityLowDialog(
    activity: Activity?,
    onContinue : () -> Unit,
  ) {
    dialog?.dismissIfShowingSafely(activity)
    activity?.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
      dialog =
        Dialog(activity).apply {
          requestWindowFeature(Window.FEATURE_NO_TITLE)
          setContentView(R.layout.low_resolution_image_dialog)
          window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
          window?.setLayout(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
          )
          setCancelable(false)

          val actionOk = findViewById<Button>(R.id.btnOk)

          actionOk?.setOnClickListener {
            dismiss()
            onContinue.invoke()
          }

          setOnDismissListener { dialog = null }
          show()
        }
    }
  }

  private fun Dialog.dismissIfShowingSafely(activity: Activity?) {
    if (activity == null || activity.isFinishing || activity.isDestroyed) return
    if (isShowing && window?.decorView?.isAttachedToWindow == true) {
      dismiss()
    }
  }

}

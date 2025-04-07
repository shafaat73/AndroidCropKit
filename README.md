
Android Crop Kit
=====================

- **Powerful** (Zoom, Rotation, Multi-Source)
- **Customizable** (Shape, Limits, Style)
- **Optimized** (Async, Sampling, Matrix)
- **Simple** image cropping library for Android

## Add to your project

```groovy
dependencies {
  implementation(io.github.shafaat73:android-crop-kit:1.0.2")
}
```

## Using the Library

There are 3 ways of using the library. Check out the sample app for all details.

### [1. Calling crop directly](./sample/src/main/kotlin/com/canhub/cropper/sample/SampleCropFragment.kt)

**Note:** This way is deprecated and will be removed in future versions. The path forward is to write your own Activity, handle all the `Uri` stuff yourself and use `CropImageView`.

```kotlin
class MainActivity : AppCompatActivity() {
  private val cropImage = registerForActivityResult(CropImageContract()) { result ->
    if (result.isSuccessful) {
      // Use the cropped image URI.
      val croppedImageUri = result.uriContent
      val croppedImageFilePath = result.getUriFilePath(this) // optional usage
      // Process the cropped image URI as needed.
    } else {
      // An error occurred.
      val exception = result.error
      // Handle the error.
    }
  }

  private fun startCrop() {
    // Start cropping activity with guidelines.
    cropImage.launch(
      CropImageContractOptions(
        cropImageOptions = CropImageOptions(
          guidelines = Guidelines.ON
        )
      )
    )

    // Start cropping activity with gallery picker only.
    cropImage.launch(
      CropImageContractOptions(
        pickImageContractOptions = PickImageContractOptions(
          includeGallery = true,
          includeCamera = false
        )
      )
    )

    // Start cropping activity for a pre-acquired image with custom settings.
    cropImage.launch(
      CropImageContractOptions(
        uri = imageUri,
        cropImageOptions = CropImageOptions(
          guidelines = Guidelines.ON,
          outputCompressFormat = Bitmap.CompressFormat.PNG
        )
      )
    )
  }

  // Call the startCrop function when needed.
}
```

### [2. Using CropView](./sample/src/main/kotlin/com/canhub/cropper/sample/SampleUsingImageViewFragment.kt)

**Note:** This is the only way forward, add `CropImageView` into your own activity and do whatever you wish. Checkout the sample for more details.

```xml
<!-- Image Cropper fill the remaining available height -->
<com.canhub.cropper.CropImageView
  android:id="@+id/cropImageView"
  android:layout_width="match_parent"
  android:layout_height="0dp"
  android:layout_weight="1"
  />
```

- Set image to crop

```kotlin
cropImageView.setImageUriAsync(uri)
// Or prefer using uri for performance and better user experience.
cropImageView.setImageBitmap(bitmap)
```

- Get cropped image

```kotlin
// Subscribe to async event using cropImageView.setOnCropImageCompleteListener(listener)
cropImageView.getCroppedImageAsync()
// Or.
val cropped: Bitmap = cropImageView.getCroppedImage()
```

### [3. Extend to make a custom activity](./sample/src/main/kotlin/com/canhub/cropper/sample/SampleCustomActivity.kt)

**Note:** This way is also deprecated and will be removed in future versions. The path forward is to write your own Activity, handle all the `Uri` stuff yourself and use `CropImageView`.

If you want to extend the `CropImageActivity` please be aware you will need to set up your `CropImageView`

- Add `CropImageActivity` into your AndroidManifest.xml
```xml
<!-- Theme is optional and only needed if default theme has no action bar. -->
<activity
  android:name="com.canhub.cropper.CropImageActivity"
  android:theme="@style/Base.Theme.AppCompat"
  />
```

- Set up your `CropImageView` after call `super.onCreate(savedInstanceState)`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setCropImageView(binding.cropImageView)
}
```

#### Custom dialog for image source pick

When calling crop directly the library will prompt a dialog for the user choose between gallery or camera (If you keep both enable).
We use the Android default AlertDialog for this. If you wanna customised it with your app theme you need to override the method `showImageSourceDialog(..)` when extending the activity _(above)_

```kotlin
override fun showImageSourceDialog(openSource: (Source) -> Unit) {
  super.showImageSourceDialog(openCamera)
}
```
When using Activity Contracts, consult with the sample app on how to use our Activity Contracts since `onActivityResult` got deprecated.

Versions after 4.3.3 have changed the APIs quite a bit, it's best to upgrade to each minor version individually, remove deprecated API usages and continue upgrading. So after using 4.3.3, upgrade to 4.4.0, upgrade to 4.5.0, 4.6.0, etc.

## License

Forked from [CanHub/Android-Image-Cropper](https://github.com/CanHub/Android-Image-Cropper)
Originally forked from [ArthurHub]([https://github.com/edmodo/cropper](https://github.com/ArthurHub/Android-Image-Cropper)).

Copyright 2016, Arthur Teplitzki, 2013, Edmodo, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the   License.
You may obtain a copy of the License in the LICENSE file, or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS   IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

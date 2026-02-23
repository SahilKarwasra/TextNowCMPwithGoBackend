package com.laara.textnowcmp.core.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (uri: String) -> Unit,
): () -> Unit {
    val callback = remember { onImageSelected }

    return remember {
        {
            val config = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }

            val picker = PHPickerViewController(configuration = config)

            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>,
                ) {
                    picker.dismissViewControllerAnimated(true, null)

                    val result = (didFinishPicking.firstOrNull() as? PHPickerResult) ?: return
                    result.itemProvider.loadFileRepresentationForTypeIdentifier(
                        typeIdentifier = "public.image",
                    ) { url, _ ->
                        url?.absoluteString?.let { callback(it) }
                    }
                }
            }

            picker.delegate = delegate

            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
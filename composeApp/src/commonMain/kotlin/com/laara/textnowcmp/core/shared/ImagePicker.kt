package com.laara.textnowcmp.core.shared

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(
       onImageSelected: (uri: String) -> Unit
): () -> Unit
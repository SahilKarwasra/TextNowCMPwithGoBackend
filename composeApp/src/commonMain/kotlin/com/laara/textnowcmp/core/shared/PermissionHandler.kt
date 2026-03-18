package com.laara.textnowcmp.core.shared

import androidx.compose.runtime.Composable

@Composable
expect fun RequestContactsPermission(
    onPermissionResult: (granted: Boolean) -> Unit,
)

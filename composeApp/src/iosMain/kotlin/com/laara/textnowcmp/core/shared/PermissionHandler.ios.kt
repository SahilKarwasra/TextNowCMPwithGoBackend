package com.laara.textnowcmp.core.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun RequestContactsPermission(
    onPermissionResult: (granted: Boolean) -> Unit,
) {
    // On iOS, CNContactStore permission is requested automatically
    // when ContactsReader accesses the store.
    // We just report as granted here — the system dialog will show on first access.
    LaunchedEffect(Unit) {
        onPermissionResult(true)
    }
}

package com.laara.textnowcmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.laara.textnowcmp.config.navigation.AppNavigation
import com.laara.textnowcmp.core.theme.TextNowCMPTheme
import org.koin.compose.KoinContext

@Composable
@Preview
fun App() {
    TextNowCMPTheme {
        AppNavigation()
    }
}
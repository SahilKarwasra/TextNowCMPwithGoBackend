package com.laara.textnowcmp

import androidx.compose.ui.window.ComposeUIViewController
import com.laara.textnowcmp.config.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }
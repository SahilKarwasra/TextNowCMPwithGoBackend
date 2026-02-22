package com.laara.textnowcmp.features.splash.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laara.textnowcmp.core.theme.TextNowCMPTheme
import com.laara.textnowcmp.core.util.ObserveAsEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import textnowcmp.composeapp.generated.resources.Res
import textnowcmp.composeapp.generated.resources.textnowdark
import textnowcmp.composeapp.generated.resources.textnowlight
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

private val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ExpoIn  = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

@Composable
fun SplashRoot(
    viewModel: SplashViewModel = viewModel(),
    navigateToTop: (Any) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) {
        when (it) {
            is SplashEvent.Navigate -> navigateToTop(it.navKey)
        }
    }

    SplashScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SplashScreen(
    state: SplashState,
    onAction: (SplashAction) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.88f) }
    val glowAlpha = remember { Animatable(0f) }

    val exitAlpha by animateFloatAsState(
        targetValue = if (state.isNavigatingAway) 0f else 1f,
        animationSpec = tween(durationMillis = 500, easing = ExpoIn),
        label = "exitAlpha",
    )
    val exitScale by animateFloatAsState(
        targetValue = if (state.isNavigatingAway) 1.04f else 1f,
        animationSpec = tween(durationMillis = 500, easing = ExpoIn),
        label = "exitScale",
    )

    LaunchedEffect(Unit) {
        launch {
            glowAlpha.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }
        delay(100)
        launch {
            logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            logoScale.animateTo(1.04f, tween(600, easing = ExpoOut))
            logoScale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = exitAlpha
                    scaleX = exitScale
                    scaleY = exitScale
                },
            contentAlignment = Alignment.Center,
        ) {

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(glowAlpha.value)
            ) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.10f),
                            primaryColor.copy(alpha = 0f),
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.52f,
                    )
                )
            }

            Image(
                painter = painterResource(
                    resource = if (isDark) Res.drawable.textnowdark
                    else Res.drawable.textnowlight
                ),
                contentDescription = "TextNow Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenLightPreview() {
    TextNowCMPTheme(darkTheme = false) {
        SplashScreen(state = SplashState(), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenDarkPreview() {
    TextNowCMPTheme(darkTheme = true) {
        SplashScreen(state = SplashState(), onAction = {})
    }
}
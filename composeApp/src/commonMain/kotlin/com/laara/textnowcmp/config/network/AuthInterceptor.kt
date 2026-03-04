package com.laara.textnowcmp.config.network

import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.core.util.ui.UiEventController
import com.laara.textnowcmp.core.util.ui.UiEvent
import com.laara.textnowcmp.features.auth.data.remote.AuthApi
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

fun HttpClientConfig<*>.installAuthInterceptor(
    tokenProvider: TokenProvider,
    authApi: AuthApi,
) {
    install(Auth) {
        bearer {
            loadTokens {
                val access = tokenProvider.getAccessToken()
                val refresh = tokenProvider.getRefreshToken()
                if (access != null && refresh != null) {
                    BearerTokens(access, refresh)
                } else {
                    null
                }
            }

            refreshTokens {
                val currentRefresh = tokenProvider.getRefreshToken()
                if (currentRefresh.isNullOrBlank()) {
                    // No refresh token available — session is fully expired
                    tokenProvider.clearTokens()
                    UiEventController.trySend(UiEvent.SessionExpired)
                    return@refreshTokens null
                }

                val result = authApi.refreshToken(currentRefresh)
                if (result != null) {
                    // Successfully refreshed — persist new tokens
                    tokenProvider.updateTokens(
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                    )
                    BearerTokens(result.accessToken, result.refreshToken)
                } else {
                    // Refresh failed (expired or invalid) — log user out
                    tokenProvider.clearTokens()
                    UiEventController.trySend(UiEvent.SessionExpired)
                    null
                }
            }

            sendWithoutRequest { true }
        }
    }
}

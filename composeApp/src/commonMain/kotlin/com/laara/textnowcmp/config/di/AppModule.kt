package com.laara.textnowcmp.config.di

import com.laara.textnowcmp.config.network.installAuthInterceptor
import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.features.auth.data.remote.AuthApi
import com.laara.textnowcmp.features.contacts.data.remote.ContactsApi
import com.laara.textnowcmp.features.contacts.data.repository.ContactsRepositoryImpl
import com.laara.textnowcmp.features.contacts.domain.repository.ContactsRepository
import com.laara.textnowcmp.features.auth.data.repository.AuthRepositoryImpl
import com.laara.textnowcmp.features.auth.domain.repository.AuthRepository
import com.laara.textnowcmp.features.auth.presentation.login.LoginViewModel
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.OtpVerificationViewModel
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail.PersonalDetailsViewModel
import com.laara.textnowcmp.features.home.presentation.newChat.NewChatViewModel
import com.laara.textnowcmp.features.splash.presentation.SplashViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

const val BASE_URL = "http://10.0.2.2:3000/api/v1"

val sharedModule = module {
    includes(platformModule)

    singleOf(::TokenProvider)
    single<HttpClient>(named("plain")) { createPlainHttpClient(get()) }
    single {
        AuthApi(
            plainClient = get(named("plain")),
            baseUrl = BASE_URL,
        )
    }

    single<HttpClient> {
        createAuthenticatedHttpClient(
            engine = get(),
            tokenProvider = get(),
            authApi = get(),
        )
    }

    singleOf(::AuthRepositoryImpl).bind<AuthRepository>()

    // Contacts
    single { ContactsApi(baseUrl = BASE_URL) }
    singleOf(::ContactsRepositoryImpl).bind<ContactsRepository>()

    // ViewModels
    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::OtpVerificationViewModel)
    viewModelOf(::PersonalDetailsViewModel)
    viewModelOf(::NewChatViewModel)
}

fun createPlainHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}

fun createAuthenticatedHttpClient(
    engine: HttpClientEngine,
    tokenProvider: TokenProvider,
    authApi: AuthApi,
): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        installAuthInterceptor(tokenProvider, authApi)
    }
}
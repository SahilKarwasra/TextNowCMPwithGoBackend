package com.laara.textnowcmp.config.di

import com.laara.textnowcmp.features.auth.presentation.login.LoginViewModel
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.OtpVerificationViewModel
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail.PersonalDetailsViewModel
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
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    includes(platformModule)
    singleOf(::createHttpClient)
    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::OtpVerificationViewModel)
    viewModelOf(::PersonalDetailsViewModel)
}

fun createHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS   // Use LogLevel.BODY for debugging
        }
    }
}
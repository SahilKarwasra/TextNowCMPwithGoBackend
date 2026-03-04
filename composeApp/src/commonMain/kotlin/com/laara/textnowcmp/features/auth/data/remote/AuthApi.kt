package com.laara.textnowcmp.features.auth.data.remote

import com.laara.textnowcmp.config.network.BaseResponse
import com.laara.textnowcmp.features.auth.data.remote.dto.RefreshTokenRequest
import com.laara.textnowcmp.features.auth.data.remote.dto.RefreshTokenResponse
import com.laara.textnowcmp.features.auth.data.remote.dto.SaveProfileRequest
import com.laara.textnowcmp.features.auth.data.remote.dto.SendOtpRequest
import com.laara.textnowcmp.features.auth.data.remote.dto.VerifyOtpRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthApi(
    private val plainClient: HttpClient,
    private val baseUrl: String,
) : KoinComponent {

    private val authenticatedClient: HttpClient by inject()

    suspend fun sendOtp(phone: String): HttpResponse {
        return plainClient.post("$baseUrl/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(SendOtpRequest(phone = phone))
        }
    }

    suspend fun verifyOtp(phone: String, otp: String): HttpResponse {
        return plainClient.post("$baseUrl/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(phone = phone, otp = otp))
        }
    }

    suspend fun saveProfile(name: String, email: String): HttpResponse {
        return authenticatedClient.post("$baseUrl/profile") {
            contentType(ContentType.Application.Json)
            setBody(SaveProfileRequest(name = name, email = email))
        }
    }

    suspend fun refreshToken(refreshToken: String): RefreshTokenResponse? {
        return try {
            val response = plainClient.post("$baseUrl/auth/refresh-token") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }

            val body = response.body<BaseResponse<RefreshTokenResponse>>()
            if (body.success && body.data != null) {
                body.data
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

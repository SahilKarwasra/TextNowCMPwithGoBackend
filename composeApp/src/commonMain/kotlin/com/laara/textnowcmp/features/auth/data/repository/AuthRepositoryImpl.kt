package com.laara.textnowcmp.features.auth.data.repository

import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.config.network.safeCall
import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.features.auth.data.remote.AuthApi
import com.laara.textnowcmp.features.auth.data.remote.dto.SaveProfileResponse
import com.laara.textnowcmp.features.auth.data.remote.dto.SendOtpResponse
import com.laara.textnowcmp.features.auth.data.remote.dto.VerifyOtpResponse
import com.laara.textnowcmp.features.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenProvider: TokenProvider,
) : AuthRepository {

    override suspend fun sendOtp(phone: String): Result<SendOtpResponse, DataError.Remote> {
        return safeCall {
            authApi.sendOtp(phone)
        }
    }

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
    ): Result<VerifyOtpResponse, DataError.Remote> {
        val result = safeCall<VerifyOtpResponse> {
            authApi.verifyOtp(phone, otp)
        }

        // On success, persist the tokens
        if (result is Result.Success) {
            tokenProvider.updateTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken,
            )
        }

        return result
    }

    override suspend fun saveProfile(
        name: String,
        email: String,
    ): Result<SaveProfileResponse, DataError.Remote> {
        return safeCall {
            authApi.saveProfile(name, email)
        }
    }
}

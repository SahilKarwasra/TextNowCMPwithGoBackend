package com.laara.textnowcmp.features.auth.domain.repository

import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.features.auth.data.remote.dto.SaveProfileResponse
import com.laara.textnowcmp.features.auth.data.remote.dto.SendOtpResponse
import com.laara.textnowcmp.features.auth.data.remote.dto.VerifyOtpResponse

interface AuthRepository {

    suspend fun sendOtp(phone: String): Result<SendOtpResponse, DataError.Remote>

    suspend fun verifyOtp(phone: String, otp: String): Result<VerifyOtpResponse, DataError.Remote>

    suspend fun saveProfile(name: String, email: String): Result<SaveProfileResponse, DataError.Remote>
}

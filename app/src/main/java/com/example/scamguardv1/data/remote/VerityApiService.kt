package com.example.scamguardv1.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VerityApiService {

    @FormUrlEncoded
    @POST("/check-url")
    suspend fun checkUrl(
        @Field("url") url: String
    ): String

    @Multipart
    @POST("/check-qr")
    suspend fun checkQr(
        @Part file: MultipartBody.Part
    ): String
}

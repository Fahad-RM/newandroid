package com.tts.fieldsales.data.api

import com.google.gson.JsonElement
import com.tts.fieldsales.data.model.OdooResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface OdooApiService {

    @POST
    suspend fun call(
        @Url url: String,
        @Body body: Map<String, Any>
    ): Response<OdooResponse<JsonElement>>

    @POST
    suspend fun callRaw(
        @Url url: String,
        @Body body: Map<String, Any>
    ): Response<JsonElement>
}

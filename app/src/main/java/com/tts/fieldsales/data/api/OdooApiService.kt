package com.tts.fieldsales.data.api

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.tts.fieldsales.data.model.OdooResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Typed request body — avoids the Retrofit wildcard type error
 * that occurs when using Map<String, Any> directly.
 */
data class JsonRpcBody(
    @SerializedName("jsonrpc") val jsonrpc: String = "2.0",
    @SerializedName("method")  val method: String = "call",
    @SerializedName("id")      val id: Int = 1,
    @SerializedName("params")  val params: @JvmSuppressWildcards Any
)

interface OdooApiService {

    @POST
    suspend fun call(
        @Url url: String,
        @Body body: JsonRpcBody
    ): Response<OdooResponse<JsonElement>>

    @POST
    suspend fun callRaw(
        @Url url: String,
        @Body body: JsonRpcBody
    ): Response<JsonElement>
}

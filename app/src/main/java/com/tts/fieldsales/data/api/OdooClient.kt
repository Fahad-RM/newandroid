package com.tts.fieldsales.data.api

import com.google.gson.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/** Simple in-memory cookie jar — persists session cookies across requests */
private class InMemoryCookieJar : CookieJar {
    private val store = mutableMapOf<String, List<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store[url.host] = cookies
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store.values.flatten()
    }
    fun clear() = store.clear()
}

object OdooClient {

    private var baseUrl: String = "https://placeholder.odoo.com"
    private var retrofit: Retrofit? = null
    private val cookieJar = InMemoryCookieJar()

    private val gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun initialize(url: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        if (cleanUrl != baseUrl || retrofit == null) {
            baseUrl = cleanUrl
            retrofit = Retrofit.Builder()
                .baseUrl(cleanUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
    }

    fun getService(): OdooApiService {
        return retrofit?.create(OdooApiService::class.java)
            ?: throw IllegalStateException("OdooClient not initialized. Call initialize(url) first.")
    }

    fun clearCookies() {
        cookieJar.clear()
    }

    /**
     * Builds a typed JsonRpcBody for call_kw endpoints.
     * Using JsonRpcBody avoids the Retrofit Map<String, Any> wildcard error.
     */
    fun buildJsonRpcBody(
        method: String,
        model: String,
        args: List<@JvmSuppressWildcards Any> = emptyList(),
        kwargs: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): JsonRpcBody = JsonRpcBody(
        params = mapOf(
            "model" to model,
            "method" to method,
            "args" to args,
            "kwargs" to kwargs
        )
    )

    /**
     * Builds a typed JsonRpcBody for authenticate / other top-level endpoints.
     */
    fun buildAuthBody(params: Map<String, @JvmSuppressWildcards Any>): JsonRpcBody =
        JsonRpcBody(params = params)
}

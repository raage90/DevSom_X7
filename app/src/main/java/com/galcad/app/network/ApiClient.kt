package com.galcad.app.network

import com.galcad.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var service: ApiService? = null
    private var currentBaseUrl: String? = null

    /**
     * Builds (or rebuilds, if the base URL changed) the Retrofit client.
     * Call this once at app startup after UrlResolver has determined the
     * real API address. installId is the same anonymous per-install ID
     * used for analytics (DeviceIdentity) -- reused here as part of the
     * request signature.
     */
    fun init(baseUrl: String, installId: String) {
        if (service != null && currentBaseUrl == baseUrl) return
        currentBaseUrl = baseUrl

        val signingInterceptor = Interceptor { chain ->
            val original = chain.request()
            val signed = RequestSigner.sign(
                method = original.method,
                path = original.url.encodedPath,
                installId = installId,
                secret = BuildConfig.APP_SIGNING_SECRET
            )
            val request = original.newBuilder()
                .addHeader("X-Install-Id", signed.installId)
                .addHeader("X-Timestamp", signed.timestamp)
                .addHeader("X-Nonce", signed.nonce)
                .addHeader("X-Signature", signed.signature)
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Only log in debug builds -- never log request/response bodies
            // in a release build, since that could leak tokens into device logs.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(signingInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(ApiService::class.java)
    }

    fun get(): ApiService = service
        ?: throw IllegalStateException("ApiClient.init() must be called before use, normally from SplashActivity at startup.")
}

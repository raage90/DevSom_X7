package com.galcad.app.network

import com.galcad.app.data.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/app-version")
    suspend fun getAppVersion(): Response<AppVersionInfo>

    @GET("api/settings")
    suspend fun getSettings(): Response<AppSettings>

    @GET("api/categories")
    suspend fun getCategories(@Query("media_type") mediaType: String): Response<List<Category>>

    @GET("api/videos")
    suspend fun getVideos(@Query("category_id") categoryId: Int?): Response<List<VideoListItem>>

    @GET("api/videos/all")
    suspend fun getAllVideos(): Response<List<VideoListItem>>

    @GET("api/videos/{id}/play")
    suspend fun playVideo(@Path("id") id: Int): Response<VideoPlayResponse>

    @GET("api/audio")
    suspend fun getAudio(@Query("category_id") categoryId: Int?): Response<List<AudioListItem>>

    @GET("api/audio/{id}/play")
    suspend fun playAudio(@Path("id") id: Int): Response<AudioPlayResponse>

    @GET("api/news")
    suspend fun getNews(): Response<List<NewsItem>>

    @GET("api/news/{id}")
    suspend fun getNewsDetail(@Path("id") id: Int): Response<NewsDetail>

    @POST("api/contact")
    suspend fun sendContactMessage(@Body body: ContactMessageRequest): Response<Unit>

    @POST("api/track")
    suspend fun track(@Body body: TrackRequest): Response<Unit>
}

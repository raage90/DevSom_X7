package com.galcad.app.data

import com.google.gson.annotations.SerializedName

// Matches a row from /api/categories -- a folder, which may contain sub-folders
data class Category(
    val id: Int,
    val name: String,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("media_type") val mediaType: String,
    val children: List<Category> = emptyList()
)

// A video listed inside a folder (before playback is requested)
data class VideoListItem(
    val id: Int,
    val title: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
    val views: Long
)

// Response from GET /api/videos/:id/play
data class VideoPlayResponse(
    val title: String,
    @SerializedName("playback_url") val playbackUrl: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int,
    @SerializedName("next_video") val nextVideo: NextVideoRef?
)

data class NextVideoRef(
    val id: Int,
    val title: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?
)

// An audio item listed inside a folder
data class AudioListItem(
    val id: Int,
    val title: String,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
    val views: Long
)

data class AudioPlayResponse(
    val title: String,
    @SerializedName("playback_url") val playbackUrl: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int
)

// A news post in the feed
data class NewsItem(
    val id: Int,
    val title: String,
    val body: String,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("video_id") val videoId: Int?,
    @SerializedName("audio_id") val audioId: Int?,
    @SerializedName("video_title") val videoTitle: String?,
    @SerializedName("audio_title") val audioTitle: String?,
    val views: Long,
    @SerializedName("created_at") val createdAt: String
)

// Full detail for one news article, with playable links if attached
data class NewsDetail(
    val id: Int,
    val title: String,
    val body: String,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("video_playback_url") val videoPlaybackUrl: String?,
    @SerializedName("audio_playback_url") val audioPlaybackUrl: String?
)

// Menu labels + any future config from GET /api/settings
data class AppSettings(
    @SerializedName("menu_home_label") val menuHomeLabel: String?,
    @SerializedName("menu_video_label") val menuVideoLabel: String?,
    @SerializedName("menu_news_label") val menuNewsLabel: String?,
    @SerializedName("menu_audio_label") val menuAudioLabel: String?,
    @SerializedName("menu_contact_label") val menuContactLabel: String?
)

// Response from GET /api/app-version -- the force-update check
data class AppVersionInfo(
    @SerializedName("minimum_app_version") val minimumAppVersion: String,
    @SerializedName("latest_app_version") val latestAppVersion: String,
    @SerializedName("update_message") val updateMessage: String
)

// Body sent to POST /api/contact
data class ContactMessageRequest(
    val name: String?,
    @SerializedName("contact_info") val contactInfo: String?,
    val message: String
)

// Body sent to POST /api/track
data class TrackRequest(
    @SerializedName("device_hash") val deviceHash: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("app_version") val appVersion: String
)

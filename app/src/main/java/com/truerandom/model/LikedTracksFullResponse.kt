package com.truerandom.model

import com.google.gson.annotations.SerializedName

// --- TOP-LEVEL RESPONSE ---

data class LikedTracksFullResponse(
    val href: String?,
    val items: List<TrackWrapper>?, // List of saved tracks
    val limit: Int?,
    val next: String?, // URL to the next page of results (for paging)
    val offset: Int?,
    val previous: String?, // URL to the previous page of results
    val total: Int?
)

// --- TRACK WRAPPER (The 'item' inside the response) ---

data class TrackWrapper(
    @SerializedName("added_at")
    val addedAt: String?, // Timestamp when the track was added to the library
    val track: Track?
)

// --- TRACK OBJECT ---

data class Track(
    val id: String?,
    val name: String?,
    val uri: String?,
    val type: String?,
    val album: Album?, // Full album object
    val artists: List<Artist>?, // List of artists

    @SerializedName("available_markets")
    val availableMarkets: List<String>?,

    @SerializedName("disc_number")
    val discNumber: Int?,

    @SerializedName("duration_ms")
    val durationMs: Long?,

    val explicit: Boolean?,

    @SerializedName("external_ids")
    val externalIds: ExternalIds?, // Contains ISRC

    @SerializedName("external_urls")
    val externalUrls: ExternalUrls?,

    val href: String?,

    @SerializedName("is_local")
    val isLocal: Boolean?,

    @SerializedName("is_playable")
    val isPlayable: Boolean?,

    val popularity: Int?,

    @SerializedName("preview_url")
    val previewUrl: String?,

    @SerializedName("track_number")
    val trackNumber: Int?
)

// --- ALBUM OBJECT ---

data class Album(
    val id: String?,
    val name: String?,
    val uri: String?,
    val type: String?,

    @SerializedName("album_type")
    val albumType: String?,

    val artists: List<Artist>?,

    @SerializedName("available_markets")
    val availableMarkets: List<String>?,

    @SerializedName("external_urls")
    val externalUrls: ExternalUrls?,

    val href: String?,
    val images: List<Image>?,

    @SerializedName("is_playable")
    val isPlayable: Boolean?,

    @SerializedName("release_date")
    val releaseDate: String?,

    @SerializedName("release_date_precision")
    val releaseDatePrecision: String?,

    @SerializedName("total_tracks")
    val totalTracks: Int?
)

// --- ARTIST OBJECT (Used inside Track and Album) ---

data class Artist(
    val id: String?,
    val name: String?,
    val uri: String?,
    val type: String?,
    val href: String?,

    @SerializedName("external_urls")
    val externalUrls: ExternalUrls?
)

// --- UTILITY MODELS ---

data class Image(
    val url: String?,
    val height: Int?,
    val width: Int?
)

data class ExternalUrls(
    val spotify: String?
)

data class ExternalIds(
    val isrc: String?
)

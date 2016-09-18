package lt.gyvosistorijos

import com.squareup.moshi.Json

data class Story
(
        val id: String,
        val text: String,
        val url: String?,
        val latitude: Double,
        val longitude: Double,
        @Json(name = "header")
        val author: String?
)

package lt.gyvosistorijos

import android.os.Bundle
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
) {
    companion object Bundler {
        fun toBundle(story: Story): Bundle {
            val b = Bundle()
            b.putString("id", story.id)
            b.putString("text", story.text)
            if (null != story.url) {
                b.putString("url", story.url)
            }
            b.putDouble("latitude", story.latitude)
            b.putDouble("longitude", story.longitude)
            if (null != story.author) {
                b.putString("author", story.author)
            }
            return b
        }

        fun fromBundle(b: Bundle): Story {
            return Story(b.getString("id"),
                    b.getString("text"),
                    b.getString("url"),
                    b.getDouble("latitude"),
                    b.getDouble("longitude"),
                    b.getString("author"))
        }
    }
}

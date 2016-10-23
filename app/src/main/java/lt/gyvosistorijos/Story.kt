package lt.gyvosistorijos

import android.os.Bundle
import com.squareup.moshi.Json
import com.squareup.sqldelight.RowMapper

class Story(
        val id: String,
        val text: String,
        val url: String?,
        val latitude: Double,
        val longitude: Double,
        @Json(name = "header")
        val author: String?
) : StoryModel {
    override fun id(): String {
        return id
    }

    override fun text(): String {
        return text
    }

    override fun url(): String? {
        return url
    }

    override fun latitude(): Double {
        return latitude
    }

    override fun longitude(): Double {
        return longitude
    }

    override fun author(): String? {
        return author
    }

    companion object : StoryModel.Creator<Story> {

        val TABLE_NAME = StoryModel.TABLE_NAME
        val CREATE_TABLE = StoryModel.CREATE_TABLE
        val FACTORY = StoryModel.Factory<Story>(this)
        val SELECT_ALL = StoryModel.SELECT_ALL
        val MAPPER: RowMapper<Story> = FACTORY.select_allMapper()

        override fun create(id: String,
                            text: String,
                            url: String?,
                            latitude: Double,
                            longitude: Double,
                            author: String?): Story {
            return Story(id, text, url, latitude, longitude, author)
        }

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

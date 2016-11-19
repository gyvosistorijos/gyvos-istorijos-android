package lt.gyvosistorijos.entity.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import lt.gyvosistorijos.entity.Story

open class StoryRealm(
        @PrimaryKey
        open var id: String = "",

        open var text: String = "",
        open var url: String? = null,
        open var latitude: Double = 0.0,
        open var longitude: Double = 0.0,
        open var author: String? = null
) : RealmObject() {

    companion object Mapper {

        fun fromStory(story: Story): StoryRealm {
            return StoryRealm(
                    id = story.id,
                    text = story.text,
                    url = story.url,
                    latitude = story.latitude,
                    longitude = story.longitude,
                    author = story.author)

        }

        fun toStory(storyRealm: StoryRealm): Story {
            return Story(
                    id = storyRealm.id,
                    text = storyRealm.text,
                    url = storyRealm.url,
                    latitude = storyRealm.latitude,
                    longitude = storyRealm.longitude,
                    author = storyRealm.author
            )
        }
    }
}


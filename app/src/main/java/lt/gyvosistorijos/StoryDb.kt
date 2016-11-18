package lt.gyvosistorijos

import io.realm.Realm
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.entity.realm.StoryRealm

object StoryDb {

    private fun <T> withRealm(func: Realm.() -> T): T {
        return Realm.getDefaultInstance().use { func(it) }
    }

    fun insert(stories: List<Story>) {
        val realmStories = stories.map { s -> StoryRealm.fromStory(s) }

        withRealm {
            executeTransaction {
                delete(StoryRealm::class.java)
                copyToRealm(realmStories)
            }
        }
    }

    fun getAll(): List<Story> {
        val stories = withRealm {
            val realmStories = where(StoryRealm::class.java).findAll()
            copyFromRealm(realmStories).map { s -> StoryRealm.toStory(s) }
        }

        return stories
    }

    fun getByIds(ids: List<String>): List<Story> {
        val stories = withRealm {
            val realmStories = where(StoryRealm::class.java).
                    `in`(StoryRealm::id.name, ids.toTypedArray()).findAll()
            copyFromRealm(realmStories).map { s -> StoryRealm.toStory(s) }
        }

        return stories
    }
}

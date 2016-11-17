package lt.gyvosistorijos

import io.realm.Realm
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.entity.realm.StoryRealm

object StoryDb {

    private fun <T> withRealm(func: (realm: Realm) -> T): T {
        return Realm.getDefaultInstance().use { func(it) }
    }

    fun insert(stories: List<Story>) {
        val realmStories = stories.map { s -> StoryRealm.fromStory(s) }


        withRealm { realm ->
            realm.executeTransaction {
                realm.delete(StoryRealm::class.java)
                realm.copyToRealm(realmStories)
            }
        }

    }

    fun getAll(): List<Story> {
        val stories = withRealm { realm ->
            val realmStories = realm.where(StoryRealm::class.java).findAll()

            realm.copyFromRealm(realmStories).map { s -> StoryRealm.toStory(s) }
        }

        return stories
    }

    fun getByIds(ids: List<String>): List<Story> {
        val stories = withRealm { realm ->
            val realmStories = realm.where(StoryRealm::class.java).
                    `in`(StoryRealm::id.name, ids.toTypedArray()).findAll()

            realm.copyFromRealm(realmStories).map { s -> StoryRealm.toStory(s) }
        }

        return stories
    }
}

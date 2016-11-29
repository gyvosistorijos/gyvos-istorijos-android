package lt.gyvosistorijos

import io.realm.Realm
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.entity.VisitedStory
import lt.gyvosistorijos.entity.realm.StoryRealm
import lt.gyvosistorijos.entity.realm.VisitedStoryRealm
import timber.log.Timber
import java.util.*

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

    fun getById(id: String): Story? {
        return withRealm {
            val realmStory = where(StoryRealm::class.java)
                    .equalTo(StoryRealm::id.name, id).findFirst()

            copyFromRealm(realmStory)?.let { s -> StoryRealm.toStory(s) }
        }
    }

    fun isStoryVisited(story: Story): Boolean {
        return withRealm {
            where(VisitedStoryRealm::class.java)
                    .equalTo(VisitedStoryRealm::storyId.name, story.id).findFirst() != null
        }
    }

    fun setStoryVisited(story: Story) {
        // Nothing to do if story is visited
        if (isStoryVisited(story))
            return

        val visitedStory = VisitedStoryRealm.fromVisitedStory(
                VisitedStory(
                        id = story.id,
                        date = Calendar.getInstance().time
                ))

        withRealm {
            Timber.i("Setting ${story.id} as visited")

            executeTransaction {
                copyToRealm(visitedStory)
            }
        }
    }

    fun getVisitedStories(): List<VisitedStory> {
        return withRealm {
            val realmVisitedStories = where(VisitedStoryRealm::class.java).findAll()
            copyFromRealm(realmVisitedStories).map { VisitedStoryRealm.toVisitedStory(it) }
        }
    }
}

package lt.gyvosistorijos.entity.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.entity.VisitedStory
import java.util.*

open class VisitedStoryRealm(
        @PrimaryKey
        open var storyId: String = "",

        open var date: Date = Date()
) : RealmObject() {

    companion object {

        fun toVisitedStory(visitedStoryRealm: VisitedStoryRealm, story: Story): VisitedStory {
            return VisitedStory(
                    id = visitedStoryRealm.storyId,
                    date = visitedStoryRealm.date,
                    story = story
            )
        }

        fun fromVisitedStory(visitedStory: VisitedStory): VisitedStoryRealm {
            return VisitedStoryRealm(
                    storyId = visitedStory.id,
                    date = visitedStory.date
            )
        }

    }

}
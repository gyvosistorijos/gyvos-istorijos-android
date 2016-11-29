package lt.gyvosistorijos.entity.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import lt.gyvosistorijos.entity.VisitedStory
import java.util.*

open class VisitedStoryRealm(
        @PrimaryKey
        open var storyId: String = "",

        open var date: Date = Date()
) : RealmObject() {

    companion object {

        fun toVisitedStory(visitedStoryRealm: VisitedStoryRealm): VisitedStory {
            return VisitedStory(
                    id = visitedStoryRealm.storyId,
                    date = visitedStoryRealm.date
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
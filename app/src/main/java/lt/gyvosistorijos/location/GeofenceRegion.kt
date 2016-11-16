package lt.gyvosistorijos.location

import lt.gyvosistorijos.entity.Story

data class GeofenceRegion(val id: String, val latitude: Double, val longitude: Double) {

    object ModelMapper {
        fun from(story: Story) = GeofenceRegion(story.id, story.latitude, story.longitude)
    }

}
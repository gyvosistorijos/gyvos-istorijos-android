package lt.gyvosistorijos.utils

import android.content.Context
import android.support.v4.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import lt.gyvosistorijos.R
import lt.gyvosistorijos.StoryDb
import lt.gyvosistorijos.entity.Story

fun addTaggedStoryMarkers(context: Context, map: GoogleMap, stories: List<Story>): List<Marker> {
    val storyDrawable = ContextCompat.getDrawable(context, R.drawable.marker_story)
    val storyVisitedDrawable = ContextCompat.getDrawable(context, R.drawable.marker_story_visited)

    val storyIcon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(storyDrawable))
    val storyVisitedIcon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(storyVisitedDrawable))

    val visitedStoryIds = StoryDb.getVisitedStories().map { it.id }.toHashSet()

    return stories.map { story ->
        val icon = if (visitedStoryIds.contains(story.id)) storyVisitedIcon else storyIcon
        val marker = map.addMarker(MarkerOptions()
                .icon(icon)
                .position(LatLng(story.latitude, story.longitude)))
        marker.tag = story

        marker
    }
}

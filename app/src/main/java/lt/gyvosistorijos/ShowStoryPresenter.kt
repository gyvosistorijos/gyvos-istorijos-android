package lt.gyvosistorijos

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_show_story.view.*
import lt.gyvosistorijos.entity.Story

class ShowStoryPresenter {

    lateinit var showStoryAnimator: ValueAnimator

    fun init(container: View) {
        val imageHeight = container.resources.getDimensionPixelSize(R.dimen.image_height)
        val attractorHeightOffset =
                container.resources.getDimensionPixelOffset(R.dimen.attractor_height_offset)
        val attractorHeightDelta =
                container.resources.getDimensionPixelOffset(R.dimen.attractor_height_delta)

        showStoryAnimator = ValueAnimator.ofFloat(
                (imageHeight - attractorHeightOffset - attractorHeightDelta).toFloat(),
                (imageHeight - attractorHeightOffset).toFloat())
        showStoryAnimator.repeatCount = ValueAnimator.INFINITE
        showStoryAnimator.repeatMode = ValueAnimator.REVERSE
        showStoryAnimator.interpolator = AccelerateDecelerateInterpolator()
        showStoryAnimator.duration = 1400
        showStoryAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            container.showStoryImage.translationY = value
        }
    }

    fun showShowStory(container: View, story: Story) {
        showStoryAnimator.start()
        container.showStoryButton.visibility = View.VISIBLE
        container.showStoryImage.visibility = View.VISIBLE
        updateShowStoryButton(container, story)
    }

    fun updateShowStoryButton(container: View, story: Story) {
        Picasso.with(container.context)
                .load(story.url).fit().centerCrop().into(container.showStoryImage)
    }

    fun hideShowStory(container: View) {
        showStoryAnimator.cancel()
        container.showStoryButton.visibility = View.INVISIBLE
        container.showStoryImage.visibility = View.INVISIBLE
    }

    fun deinit() {
        showStoryAnimator.cancel()
        showStoryAnimator.removeAllUpdateListeners()
    }
}

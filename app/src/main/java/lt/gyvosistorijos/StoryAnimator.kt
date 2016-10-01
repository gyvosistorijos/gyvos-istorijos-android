package lt.gyvosistorijos

import android.animation.Animator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlinx.android.synthetic.main.controller_story.view.*

class StoryAnimator {

    fun animateInStory(container: View) {
        container.storyImage.scaleX = 0.8f
        container.storyImage.scaleY = 0.8f
        container.storyImage.animate()
                .scaleX(1f).scaleY(1f)
                .setInterpolator(OvershootInterpolator())
                .start()

        container.storyText.alpha = 0f
        container.storyText.translationY = 64f
        container.storyText.animate()
                .alpha(1f).translationY(0f)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()

        container.storyAuthor.alpha = 0f
        container.storyAuthor.translationY = 64f
        container.storyAuthor.animate()
                .alpha(1f).translationY(0f)
                .setInterpolator(DecelerateInterpolator())
                .setStartDelay(200)
                .start()

        container.storyAuthorImage.alpha = 0f
        container.storyAuthorImage.translationY = 64f
        container.storyAuthorImage.animate()
                .alpha(1f).translationY(0f)
                .setInterpolator(DecelerateInterpolator())
                .setStartDelay(200)
                .start()
    }

    fun animateOutStory(container: View, listener: Animator.AnimatorListener) {
        container.storyImage.animate()
                .scaleX(0f).scaleY(0f)
                .setInterpolator(AccelerateInterpolator())
                .start()

        container.storyText.animate()
                .alpha(0f).translationY(64f)
                .setInterpolator(AccelerateInterpolator())
                .setListener(listener)
                .start()

        container.storyAuthor.animate()
                .alpha(0f).translationY(64f)
                .setInterpolator(AccelerateInterpolator())
                .setStartDelay(0)
                .start()

        container.storyAuthorImage.animate()
                .alpha(0f).translationY(64f)
                .setInterpolator(AccelerateInterpolator())
                .setStartDelay(0)
                .start()
    }

}
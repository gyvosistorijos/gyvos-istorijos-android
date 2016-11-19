package lt.gyvosistorijos

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.controller_story.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.utils.AppEvent

class StoryController(args: Bundle) : Controller(args) {

    companion object {
        val SCREEN_NAME = "Story"
    }

    constructor(story: Story) : this(Story.toBundle(story))

    private val story = Story.fromBundle(args)
    private val storyAnimator: StoryAnimator = StoryAnimator()

    private var animatingOut = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_story, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        (activity as MainActivity).map.isMyLocationEnabled = false

        view.hideStoryButton.setOnClickListener { clickHideButton() }
        bindStory(story)
    }

    private fun bindStory(story: Story) {
        if (!story.url.isNullOrBlank()) {
            view!!.storyImage.visibility = View.VISIBLE
            Picasso.with(activity).load(story.url)
                    .fit().centerCrop()
                    .placeholder(R.color.imagePlaceholder)
                    .into(view!!.storyImage)
        } else {
            view!!.storyImage.visibility = View.GONE
        }

        view!!.storyText.text = @Suppress("DEPRECATION") (Html.fromHtml(story.text))
        Linkify.addLinks(view!!.storyText, Linkify.WEB_URLS)

        if (!story.author.isNullOrBlank()) {
            view!!.storyAuthor.text = story.author
            view!!.storyAuthor.visibility = View.VISIBLE
            view!!.storyAuthorImage.visibility = View.VISIBLE
        } else {
            view!!.storyAuthor.visibility = View.GONE
            view!!.storyAuthorImage.visibility = View.GONE
        }

        storyAnimator.animateInStory(view!!.storyContainer)

        view!!.storyScrim.alpha = 0f
        view!!.storyScrim.animate().alpha(1f)

        view!!.hideStoryButton.alpha = 0f
        view!!.hideStoryButton.animate().alpha(1f)
    }

    internal fun clickHideButton() {
        if (animatingOut) {
            return
        }

        animatingOut = true
        view!!.storyScrim.animate().alpha(0f)
        view!!.hideStoryButton.animate().alpha(0f)
        storyAnimator.animateOutStory(view!!.storyContainer, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // render last frame of animation before popping
                view!!.post { router.popCurrentController() }
            }
        })
    }

    override fun handleBack(): Boolean {
        clickHideButton()
        return true
    }
}

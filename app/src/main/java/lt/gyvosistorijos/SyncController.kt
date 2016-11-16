package lt.gyvosistorijos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import kotlinx.android.synthetic.main.controller_sync.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.GeofenceRegion
import lt.gyvosistorijos.utils.AppEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SyncController : Controller() {

    companion object {
        val SCREEN_NAME = "Sync"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_sync, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        view.syncRetryButton.setOnClickListener { syncStories() }
        syncStories()
    }

    private fun syncStories() {
        view!!.syncProgress.show()
        view!!.syncRetryButton.visibility = View.GONE
        view!!.syncPrompt.visibility = View.GONE

        Api.getStoriesService().listStories().enqueue(object : Callback<List<Story>> {
            override fun onResponse(call: Call<List<Story>>, response: Response<List<Story>>) {
                if (response.isSuccessful) {
                    onSuccess(response.body())
                } else {
                    onFailure()
                }
            }

            override fun onFailure(call: Call<List<Story>>, t: Throwable) {
                onFailure()
            }
        })
    }

    private fun onSuccess(stories: List<Story>) {
        if (!isAttached) {
            return
        }

        StoryDb.insert(stories)


        setGeofencingStories(stories)

        router.replaceTopController(RouterTransaction.with(PermissionsController()))
    }

    private fun setGeofencingStories(stories: List<Story>) {
        val geofenceRegions = stories.map { s -> GeofenceRegion.ModelMapper.from(s) }

        (activity as MainActivity).geofenceHelper.setGeofenceRegions(geofenceRegions)
    }

    private fun onFailure() {
        if (!isAttached) {
            return
        }

        if (StoryDb.getAll().isNotEmpty()) {
            router.replaceTopController(RouterTransaction.with(PermissionsController()))
            return
        }

        view!!.syncProgress.hide()
        view!!.syncRetryButton.visibility = View.VISIBLE
        view!!.syncPrompt.visibility = View.VISIBLE
    }
}

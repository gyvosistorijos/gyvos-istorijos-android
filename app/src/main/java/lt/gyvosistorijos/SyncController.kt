package lt.gyvosistorijos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import kotlinx.android.synthetic.main.controller_sync.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SyncController : Controller() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_sync, container, false)
        return view
    }

    override fun onAttach(view: View) {
        view.syncRetryButton.setOnClickListener { syncStories() }
        syncStories()
    }

    private fun syncStories() {
        view.syncProgress.show()
        view.syncRetryButton.visibility = View.GONE
        view.syncPrompt.visibility = View.GONE

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

        val db = StoryDb(applicationContext)
        for (story in stories) {
            db.insert(story)
        }

        router.replaceTopController(RouterTransaction.with(PermissionsController()))
    }

    private fun onFailure() {
        if (!isAttached) {
            return
        }

        val db = StoryDb(applicationContext)
        if (db.getAll().isNotEmpty()) {
            router.replaceTopController(RouterTransaction.with(PermissionsController()))
            return
        }

        view.syncProgress.hide()
        view.syncRetryButton.visibility = View.VISIBLE
        view.syncPrompt.visibility = View.VISIBLE
    }
}

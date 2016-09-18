package lt.gyvosistorijos

import android.app.Application
import com.mapbox.mapboxsdk.MapboxAccountManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MapboxAccountManager.start(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
    }

}

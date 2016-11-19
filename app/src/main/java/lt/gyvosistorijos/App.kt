package lt.gyvosistorijos

import android.app.Application
import com.mapbox.mapboxsdk.MapboxAccountManager
import io.realm.Realm
import lt.gyvosistorijos.utils.CrashReportingTree
import timber.log.Timber
import timber.log.Timber.DebugTree

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        Realm.init(this)

        MapboxAccountManager.start(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
    }

}

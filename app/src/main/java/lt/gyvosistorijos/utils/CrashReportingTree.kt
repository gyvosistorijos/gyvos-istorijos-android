package lt.gyvosistorijos.utils

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        FirebaseCrash.log("$tag: $message")

        t?.let { FirebaseCrash::report }
    }

}
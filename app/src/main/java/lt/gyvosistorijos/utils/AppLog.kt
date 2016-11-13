package lt.gyvosistorijos.utils


import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import lt.gyvosistorijos.BuildConfig

/**
 * AppLog class, use Throwable to find class and method
 */
object AppLog {

    val ALLOW_LOG = BuildConfig.DEBUG

    /**
     * Log String error

     * @param value
     */
    private fun error(value: String) {
        if (ALLOW_LOG) {
            Log.e(tag, value)
        }
        logToFirebase(value)
    }

    /**
     * Log String debug

     * @param value
     */
    private fun debug(value: String, allow: Boolean) {
        if (allow) {
            Log.d(tag, value)
        }
        logToFirebase(value)
    }

    private fun logToFirebase(value: String) {
        FirebaseCrash.log("$tag: $value")
    }

    // create throwable
    // build tag from throwable
    private val tag: String
        get() {
            try {
                val t = Throwable()
                val elements = t.stackTrace
                val classComponents = elements[3].className.split("\\.".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

                val tag = classComponents[classComponents.size - 1] + "(" + elements[3].methodName + ":" + elements[3].lineNumber + ")"
                return tag
            } catch (e: Exception) {

            }

            return "UNKNOWN"
        }


    /**
     * Log String warning

     * @param value
     */
    private fun warning(value: String) {
        if (ALLOW_LOG) {
            Log.w(tag, value)
        }
        logToFirebase(value)
    }

    /**
     * Log String info

     * @param value
     */
    private fun info(value: String) {
        if (ALLOW_LOG) {
            Log.i(tag, value)
        }
        logToFirebase(value)
    }

    /**
     * Log warning

     * @param value
     */
    fun w(value: String) {
        AppLog.warning(value)
    }

    /**
     * Log info

     * @param value
     */
    fun i(value: String) {
        AppLog.info(value)
    }

    /**
     * Log String error

     * @param value
     */
    fun e(value: String) {
        AppLog.error(value)
    }

    /**
     * Log String debug

     * @param value
     */
    fun d(value: String) {
        AppLog.debug(value, ALLOW_LOG)
    }


    /**
     * Log Exception error with stacktrace flag

     * @param value
     */
    fun e(value: Throwable) {
        // Log Exception info
        AppLog.error(value.toString())

        // display stack trace
        if (ALLOW_LOG) {
            value.printStackTrace()
        }

        FirebaseCrash.report(value)
    }

    /**
     * Log stack trace debug

     * @param trace
     */
    fun d(trace: Array<StackTraceElement>) {
        for (element in trace) {
            AppLog.debug(element.toString(), ALLOW_LOG)
        }
    }

    /**
     * Log Exception debug with stacktrace flag

     * @param value
     */
    fun d(value: Exception) {
        // Log Exception info
        AppLog.debug(value.toString(), ALLOW_LOG)

        if (ALLOW_LOG) {
            value.printStackTrace()
        }
    }


    /**
     * Log double/float debug

     * @param value
     */
    fun d(value: Double) {
        AppLog.debug(value.toString(), ALLOW_LOG)
    }

    /**
     * Log long/int debug

     * @param value
     */
    fun d(value: Long) {
        AppLog.debug(value.toString(), ALLOW_LOG)
    }

}
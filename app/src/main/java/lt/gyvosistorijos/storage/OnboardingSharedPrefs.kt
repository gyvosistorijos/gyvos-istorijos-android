package lt.gyvosistorijos.storage

import android.content.Context
import android.content.SharedPreferences
import lt.gyvosistorijos.utils.edit

class OnboardingSharedPrefs(context: Context,
                            val sharedPrefs: SharedPreferences
                            = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = "completed"
    }

    fun onboardingCompleted(): Boolean {
        return sharedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted() {
        sharedPrefs.edit {
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
        }
    }
}

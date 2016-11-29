package lt.gyvosistorijos.utils

import android.os.Bundle

inline fun Bundle.set(func: Bundle.() -> Unit): Bundle {
    func()
    return this
}

fun Bundle.bool(pair: Pair<String, Boolean>) = putBoolean(pair.first, pair.second)

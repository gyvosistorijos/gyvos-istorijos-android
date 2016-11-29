package lt.gyvosistorijos.utils

import android.content.res.Resources
import android.support.annotation.DimenRes
import android.util.TypedValue

fun Resources.getFloat(@DimenRes id: Int): Float {
    val out = TypedValue()
    getValue(id, out, true)
    return out.float
}

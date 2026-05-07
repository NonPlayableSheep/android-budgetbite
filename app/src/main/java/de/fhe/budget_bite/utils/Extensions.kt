package de.fhe.budget_bite.utils

import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import de.fhe.budget_bite.MainActivity
import java.util.Locale

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

/**
 * Hides the visibility of a [View] by setting it to [View.GONE].
 */
fun View.gone() {
    this.visibility = View.GONE
}

/**
 * Shows the visibility of a [View] by setting it to [View.VISIBLE].
 */
fun View.show() {
    this.visibility = View.VISIBLE
}

/**
 * Hides a [MenuItem] by setting its visibility to false.
 */
fun MenuItem.gone() {
    this.isVisible = false
}

/**
 * Shows a [MenuItem] by setting its visibility to true.
 */
fun MenuItem.show() {
    this.isVisible = true
}

fun Float.toCalories(): String = this.toInt().toString()

fun Float.toPrice(): String = String.format(Locale.US, "%.2f", this)

fun Float.toOneDecimal(): String = String.format(Locale.US, "%.1f", this)
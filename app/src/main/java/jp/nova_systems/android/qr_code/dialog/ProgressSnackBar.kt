package jp.nova_systems.android.qr_code.dialog

import android.app.Activity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import jp.nova_systems.android.qr_code.R

class ProgressSnackBar(private val activity: Activity, private val text: String) {
    fun create(): Snackbar {
        val container = activity.findViewById<ConstraintLayout>(R.id.container)
        return Snackbar.make(container, text, Snackbar.LENGTH_INDEFINITE).apply {
            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            val layout = textView.parent as ViewGroup
            val linearLayout =  LinearLayout(activity).apply {
                val progressBar = ProgressBar(activity)
                layoutParams = LinearLayout.LayoutParams(130, 130)
                setPadding(0, 10, 0, 10)
                addView(progressBar)
            }
            layout.addView(linearLayout, 0)
        }
    }
}
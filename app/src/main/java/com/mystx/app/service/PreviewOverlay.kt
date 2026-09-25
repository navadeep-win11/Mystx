package com.mystx.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PreviewOverlay(private val context: Context) {
    private var currentView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun dp(value: Int): Int {
        val density = context.resources.displayMetrics.density
        return (value * density + 0.5f).toInt()
    }

    fun show(text: String, onResult: (Boolean) -> Unit) {
        handler.post {
            dismiss()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E1E2E")) // Dark surface color
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.parseColor("#3C3C50"))
                }
            }

            val titleView = TextView(context).apply {
                this.text = "Preview Text"
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, dp(12))
            }
            layout.addView(titleView)

            val scrollView = ScrollView(context).apply {
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                layoutParams = lp
            }

            val textView = TextView(context).apply {
                this.text = text
                setTextColor(Color.parseColor("#E0E0E0"))
                textSize = 15f
                setTextIsSelectable(true)
                setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics), 1.0f)
            }
            scrollView.addView(textView)
            layout.addView(scrollView)

            val buttonLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(16), 0, 0)
                gravity = Gravity.END
            }

            val btnCancel = Button(context).apply {
                this.text = "Cancel"
                isAllCaps = false
                setTextColor(Color.parseColor("#F28B82"))
                background = getRippleDrawable(Color.TRANSPARENT, Color.parseColor("#33F28B82"))
                setOnClickListener {
                    dismiss()
                    onResult(false)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(8)
                }
            }

            val btnAccept = Button(context).apply {
                this.text = "Replace"
                isAllCaps = false
                setTextColor(Color.BLACK)
                background = getRippleDrawable(Color.parseColor("#8AB4F8"), Color.parseColor("#55FFFFFF"), dp(8).toFloat())
                setOnClickListener {
                    dismiss()
                    onResult(true)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            buttonLayout.addView(btnCancel)
            buttonLayout.addView(btnAccept)
            layout.addView(buttonLayout)

            layout.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                    onResult(false)
                    true
                } else {
                    false
                }
            }

            val params = WindowManager.LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
                (context.resources.displayMetrics.heightPixels * 0.5).toInt(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                dimAmount = 0.6f
            }

            try {
                wm.addView(layout, params)
                currentView = layout
            } catch (e: Exception) {
                // If it fails to add window, just auto-accept to not break flow
                onResult(true)
            }
        }
    }

    private fun getRippleDrawable(normalColor: Int, rippleColor: Int, radius: Float = 0f): RippleDrawable {
        val content = GradientDrawable().apply {
            setColor(normalColor)
            cornerRadius = radius
        }
        return RippleDrawable(ColorStateList.valueOf(rippleColor), content, null)
    }

    fun dismiss() {
        handler.post {
            currentView?.let {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                try { wm.removeView(it) } catch (e: Exception) {}
                currentView = null
            }
        }
    }
}

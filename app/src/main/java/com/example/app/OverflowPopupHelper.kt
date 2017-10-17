package com.example.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.view.menu.MenuPresenter
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow

@SuppressLint("RestrictedApi")
class OverflowPopupHelper(
        private val context: Context,
        private val menu: MenuBuilder,
        private val anchor: View,
        private val overflowOnly: Boolean
) : PopupWindow.OnDismissListener {

    companion object {
        private const val TOUCH_EPICENTER_SIZE_DP = 48
    }

    private var popup: OverflowPopup? = null
        get() = if (field == null) createPopup() else field

    var gravity = GravityCompat.END

    var presenterCallback: MenuPresenter.Callback? = null

    var onDismissListener: PopupWindow.OnDismissListener? = null

    var isShowing: Boolean = false
        get() = popup?.isShowing ?: false

    override fun onDismiss() {
        menu.close()
        popup = null
        onDismissListener?.onDismiss()
    }

    fun show(x: Int, y: Int) {
        popup?.let {
            var horizontalOffset = x

            val horizontalGravity = GravityCompat.getAbsoluteGravity(gravity,
                    ViewCompat.getLayoutDirection(anchor) and
                            Gravity.HORIZONTAL_GRAVITY_MASK)

            if (horizontalGravity == Gravity.RIGHT) horizontalOffset += anchor.width

            it.horizontalOffset = horizontalOffset
            it.verticalOffset = y

            val density = context.resources.displayMetrics.density
            val halfSize = (TOUCH_EPICENTER_SIZE_DP * density / 2).toInt()
            val epicenter = Rect(horizontalOffset - halfSize, y - halfSize,
                    horizontalOffset + halfSize, y + halfSize)

            it.epicenterBounds = epicenter
            it.show()
        }
    }

    fun show() { popup?.show() }

    fun dismiss() { if (isShowing) popup?.dismiss() }

    private fun createPopup(): OverflowPopup {
        val popup = OverflowPopup(context, anchor)

        popup.addMenu(menu)
        popup.onDismissListener = this

        popup.setCallback(presenterCallback)
        popup.gravity = gravity

        return popup
    }

}
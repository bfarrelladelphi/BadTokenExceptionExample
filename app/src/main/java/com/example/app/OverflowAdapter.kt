package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.support.v7.view.menu.MenuAdapter
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.widget.TooltipCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView


@SuppressLint("RestrictedApi")
class OverflowAdapter(
        private val menu: MenuBuilder,
        private val inflater: LayoutInflater,
        private val overflowOnly: Boolean
): MenuAdapter(menu, inflater, overflowOnly) {

    companion object {
        private const val STANDARD_MENU_ITEM = 0
        private const val TOOLBAR_MENU_ITEM = 1

        private val ENTER_ITEM_DURATION_MS = 350L
        private val ENTER_ITEM_BASE_DELAY_MS = 80L
        private val ENTER_ITEM_ADDL_DELAY_MS = 30L
        private val ENTER_STANDARD_ITEM_OFFSET_Y_DP = -10F
        private val ENTER_STANDARD_ITEM_OFFSET_X_DP = 10F

        private val BUTTON_IDS = intArrayOf(
                R.id.actionButton1,
                R.id.actionButton2,
                R.id.actionButton3,
                R.id.actionButton4,
                R.id.actionButton5)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        val view: View
        when (getItemViewType(position)) {
            TOOLBAR_MENU_ITEM -> {
                val holder: ToolbarMenuItemViewHolder
                if (convertView == null || convertView.tag !is ToolbarMenuItemViewHolder) {
                    view = inflater.inflate(R.layout.menu_item_toolbar, parent, false)
                    holder = ToolbarMenuItemViewHolder(view)
                    view.tag = holder
                    view.setTag(R.id.menuItemEnterAnimation,
                            buildToolbarItemEnterAnimator(holder.buttons))
                } else {
                    view = convertView
                    holder = view.tag as ToolbarMenuItemViewHolder
                }

                holder.bindItem(item)
            }
            else -> {
                view = super.getView(position, convertView, parent)
                if (convertView == null) {
                    view.setTag(R.id.menuItemEnterAnimation,
                            buildStandardItemEnterAnimator(view, position))
                }
            }
        }

        return view
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.hasSubMenu() && item.subMenu.size() <= 5) {
            TOOLBAR_MENU_ITEM
        } else {
            STANDARD_MENU_ITEM
        }
    }

    private class ToolbarMenuItemViewHolder(val itemView: View) {

        val buttons: Array<ImageButton> = BUTTON_IDS.map {
            itemView.findViewById<ImageButton>(it)
        }.toTypedArray()

        init {
            itemView.isFocusable = false
            itemView.isEnabled = false
        }

        fun bindItem(item: MenuItem) {
            val subMenu = item.subMenu
            for ((i, button) in buttons.withIndex()) {
                val subItem = item.subMenu.getItem(i)
                if (i < item.subMenu.size()) {
                    Log.d("OverflowAdapter", "index=$i, isEnabled=${subItem.isEnabled}, setImageDrawable=${subItem.icon}")
                    button.setImageDrawable(TintManager
                            .getTintedDrawable(itemView.context, subItem.icon))
                    button.isEnabled = subItem.isEnabled
                    button.isFocusable = subItem.isEnabled

                    if (subItem.titleCondensed.isNullOrBlank()) {
                        button.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    } else {
                        button.contentDescription = subItem.titleCondensed
                        button.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    }

                    button.setOnClickListener { /*popupWindow.onItemClick(item)*/ }
                    // button.setOnLongClickListener { view -> popupWindow.onItemLongClick(item, view) }
                    TooltipCompat.setTooltipText(button, subItem.title)

                    button.visibility = View.VISIBLE
                } else {
                    button.visibility = View.GONE
                }
            }

            itemView.isFocusable = false
            itemView.isEnabled = false
        }

    }

    private fun buildStandardItemEnterAnimator(view: View, position: Int): Animator {
        val startDelay = ENTER_ITEM_BASE_DELAY_MS +  ENTER_ITEM_ADDL_DELAY_MS * position
        val animation = AnimatorSet()
        //if (translateMenuItemsOnShow) {
        val offsetYPx = ENTER_STANDARD_ITEM_OFFSET_Y_DP * view.context.resources.displayMetrics.density
        animation.playTogether(ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, offsetYPx, 0f))
        animation.startDelay = startDelay
        /*} else {
            animation.playTogether(ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f))
            // Start delay is set to make sure disabling the animation in battery saver mode does
            // not cause the view to stay at alpha 0 on Android O.
            animation.startDelay = WebMenuAdapter.ENTER_ITEM_BASE_DELAY_MS
        }*/
        animation.duration = ENTER_ITEM_DURATION_MS
        animation.interpolator = BakedBezierInterpolator.FADE_IN_CURVE
        animation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.alpha = 0f
            }
        })
        return animation
    }

    private fun buildToolbarItemEnterAnimator(buttons: Array<out ImageView?>): Animator {
        val rtl = false
        val offsetXPx = ENTER_STANDARD_ITEM_OFFSET_X_DP *
                inflater.context.resources.displayMetrics.density * if (rtl) -1f else 1f
        val maxViewsToAnimate = buttons.size
        val animation = AnimatorSet()
        var builder: AnimatorSet.Builder? = null

        for (i in 0 until maxViewsToAnimate) {
            val startDelay = ENTER_ITEM_ADDL_DELAY_MS * i
            val view = buttons[i]
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            val translate = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_X, offsetXPx, 0F)

            alpha.startDelay = startDelay
            translate.startDelay = startDelay

            alpha.duration = ENTER_ITEM_DURATION_MS
            translate.duration = ENTER_ITEM_DURATION_MS

            if (builder == null) {
                builder = animation.play(alpha)
            } else {
                builder.with(alpha)
            }
            builder?.with(translate)
        }

        animation.startDelay = ENTER_ITEM_BASE_DELAY_MS
        animation.interpolator = BakedBezierInterpolator.FADE_IN_CURVE
        animation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                for (i in 0 until maxViewsToAnimate) {
                    buttons[i]?.alpha = 0f
                }
            }
        })

        return animation
    }

}
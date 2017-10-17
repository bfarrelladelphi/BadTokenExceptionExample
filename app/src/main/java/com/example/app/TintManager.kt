package com.example.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.LruCache
import android.util.SparseArray

object TintManager {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
            TINT_COLOR_CONTROL_NORMAL,
            TINT_COLOR_CONTROL_STATE_LIST,
            TINT_COLOR_EDIT_TEXT,
            TINT_COLOR_SWITCH_TRACK,
            TINT_COLOR_SWITCH_THUMB,
            TINT_COLOR_BUTTON,
            TINT_COLOR_SPINNER,
            COLOR_FILTER_TINT_COLOR_CONTROL_NORMAL,
            COLOR_FILTER_CONTROL_ACTIVATED,
            COLOR_FILTER_CONTROL_BACKGROUND_MULTIPLY
    ) annotation class TintMode

    const val TINT_COLOR_CONTROL_NORMAL = 0L
    const val TINT_COLOR_CONTROL_STATE_LIST = 2L
    const val TINT_COLOR_EDIT_TEXT = 3L
    const val TINT_COLOR_SWITCH_TRACK = 4L
    const val TINT_COLOR_SWITCH_THUMB = 5L
    const val TINT_COLOR_BUTTON = 6L
    const val TINT_COLOR_SPINNER = 7L
    const val COLOR_FILTER_TINT_COLOR_CONTROL_NORMAL = 8L
    const val COLOR_FILTER_CONTROL_ACTIVATED = 9L
    const val COLOR_FILTER_CONTROL_BACKGROUND_MULTIPLY = 10L


    private val tintStateLists: SparseArray<ColorStateList> by lazy {
        SparseArray<ColorStateList>()
    }

    private val COLOR_FILTER_CACHE = ColorFilterLruCache(6)

    private var defaultColorStateList: ColorStateList? = null

    fun getTintedDrawable(context: Context, @DrawableRes resId: Int, @TintMode tintMode: Long = TINT_COLOR_CONTROL_NORMAL): Drawable? {
        var drawable = ContextCompat.getDrawable(context, resId)

        drawable?.let {
            drawable = drawable.mutate()

            val tintList = getTintList(context, tintMode)

            if (tintList != null) {
                drawable = DrawableCompat.wrap(drawable)
                DrawableCompat.setTintList(drawable, tintList)
            } else {
                tintDrawableUsingColorFilter(context, resId, drawable)
            }
        }

        return drawable
    }

    fun getTintedDrawable(context: Context, @DrawableRes drawableId: Int, @ColorRes tintId: Int): Drawable? {
        var drawable = ContextCompat.getDrawable(context, drawableId)

        drawable?.let {
            drawable = drawable.mutate()

            val tintList = getTintList(context, tintId)

            if (tintList != null) {
                drawable = DrawableCompat.wrap(drawable)
                DrawableCompat.setTintList(drawable, tintList)
            } else {
                tintDrawableUsingColorFilter(context, drawableId, drawable)
            }
        }

        return drawable
    }

    fun getTintedDrawable(context: Context, drawable: Drawable?, @TintMode tintMode: Long = TINT_COLOR_CONTROL_NORMAL): Drawable? {
        var tintedDrawable = drawable

        tintedDrawable?.let {
            tintedDrawable = it.mutate()

            val tintList = getTintList(context, tintMode)

            if (tintList != null) {
                tintedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTintList(it, tintList)
            }
        }

        return tintedDrawable
    }

    fun getTintList(context: Context, @TintMode tintMode: Long): ColorStateList? {
        var tint = tintStateLists.get(tintMode.toInt())

        if (tint == null) {
            tint = when (tintMode) {
                TINT_COLOR_EDIT_TEXT -> createEditTextColorStateList(context)
                TINT_COLOR_SWITCH_TRACK -> createSwitchTrackColorStateList(context)
                TINT_COLOR_SWITCH_THUMB -> createSwitchThumbColorStateList(context)
                TINT_COLOR_BUTTON -> createButtonColorStateList(context)
                TINT_COLOR_SPINNER -> createSpinnerColorStateList(context)
                TINT_COLOR_CONTROL_NORMAL -> context.getThemeAttrColorStateList(R.attr.colorControlNormal)
                else -> getDefaultTintList(context)
            }

            tint?.let {
                tintStateLists.append(tintMode.toInt(), tint)
            }
        }

        return tint
    }

    fun getTintList(context: Context, @ColorRes resId: Int): ColorStateList? {
        var tint = tintStateLists.get(resId)

        if (tint == null) {
            tint = ContextCompat.getColorStateList(context, resId)
        }

        if (tint != null) {
            tintStateLists.append(resId, tint)
        }

        return tint
    }

    private fun createSwitchTrackColorStateList(context: Context): ColorStateList {
        val states = Array(3, { intArrayOf() })
        val colors = IntArray(3)
        var i = 0

        states[i] = StateSet.DISABLED
        colors[i] = context.getThemeAttrColor(android.R.attr.colorForeground, 0.1F)
        i++

        states[i] = StateSet.CHECKED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlActivated, 0.3F)
        i++

        states[i] = StateSet.ENABLED
        colors[i] = context.getThemeAttrColor(android.R.attr.colorForeground, 0.3F)

        return ColorStateList(states, colors)
    }

    private fun createSwitchThumbColorStateList(context: Context): ColorStateList {
        val states = Array(3, { intArrayOf() })
        val colors = IntArray(3)
        var i = 0

        val thumbStateList = context.getThemeAttrColorStateList(R.attr.colorSwitchThumbNormal)

        if (thumbStateList != null && thumbStateList.isStateful) {
            states[i] = StateSet.DISABLED
            colors[i] = thumbStateList.getColorForState(states[i], 0)
            i++

            states[i] = StateSet.CHECKED
            colors[i] = context.getThemeAttrColor(R.attr.colorControlActivated)
            i++

            states[i] = StateSet.ENABLED
            colors[i] = thumbStateList.defaultColor
        } else {
            states[i] = StateSet.DISABLED
            colors[i] = context.getDisabledThemeAttrColor(R.attr.colorSwitchThumbNormal)
            i++

            states[i] = StateSet.CHECKED
            colors[i] = context.getThemeAttrColor(R.attr.colorControlActivated)
            i++

            states[i] = StateSet.ENABLED
            colors[i] = context.getThemeAttrColor(R.attr.colorSwitchThumbNormal)
        }

        return ColorStateList(states, colors)
    }

    private fun createEditTextColorStateList(context: Context): ColorStateList {
        val states = Array(3, { intArrayOf() })
        val colors = IntArray(3)
        var i = 0

        states[i] = StateSet.DISABLED
        colors[i] = context.getDisabledThemeAttrColor(R.attr.colorControlNormal)
        i++

        states[i] = StateSet.NOT_PRESSED_OR_FOCUSED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlNormal)
        i++

        states[i] = StateSet.ENABLED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlActivated)

        return ColorStateList(states, colors)
    }

    private fun createButtonColorStateList(context: Context): ColorStateList {
        val states = Array(4, { intArrayOf() })
        val colors = IntArray(4)
        var i = 0

        states[i] = StateSet.DISABLED
        colors[i] = context.getDisabledThemeAttrColor(R.attr.colorButtonNormal)
        i++

        states[i] = StateSet.PRESSED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlHighlight)
        i++

        states[i] = StateSet.FOCUSED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlHighlight)
        i++

        states[i] = StateSet.ENABLED
        colors[i] = context.getThemeAttrColor(R.attr.colorButtonNormal)

        return ColorStateList(states, colors)
    }

    private fun createSpinnerColorStateList(context: Context): ColorStateList {
        val states = Array(3, { intArrayOf() })
        val colors = IntArray(3)
        var i = 0

        states[i] = StateSet.DISABLED
        colors[i] = context.getDisabledThemeAttrColor(R.attr.colorControlNormal)
        i++

        states[i] = StateSet.NOT_PRESSED_OR_FOCUSED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlNormal)
        i++

        states[i] = StateSet.ENABLED
        colors[i] = context.getThemeAttrColor(R.attr.colorControlActivated)

        return ColorStateList(states, colors)
    }

    private fun getDefaultTintList(context: Context): ColorStateList {
        var colorStateList = defaultColorStateList

        if (colorStateList != null) {
            return colorStateList
        } else {
            val colorControlNormal = context.getThemeAttrColor(R.attr.colorControlNormal)
            val colorControlActivated = context.getThemeAttrColor(R.attr.colorControlActivated)

            val states = Array(7, { intArrayOf() })
            val colors = IntArray(7)
            var i = 0

            states[i] = StateSet.DISABLED
            colors[i] = context.getDisabledThemeAttrColor(R.attr.colorControlNormal)
            i++

            states[i] = StateSet.FOCUSED
            colors[i] = colorControlActivated
            i++

            states[i] = StateSet.ACTIVATED
            colors[i] = colorControlActivated
            i++

            states[i] = StateSet.PRESSED
            colors[i] = colorControlActivated
            i++

            states[i] = StateSet.CHECKED
            colors[i] = colorControlActivated
            i++

            states[i] = StateSet.SELECTED
            colors[i] = colorControlActivated
            i++

            states[i] = StateSet.ENABLED
            colors[i] = colorControlNormal

            colorStateList = ColorStateList(states, colors)
            defaultColorStateList = colorStateList
            return colorStateList
        }
    }

    private fun tintDrawableUsingColorFilter(context: Context, @DrawableRes resId: Int, drawable: Drawable) {
        val tintMode: PorterDuff.Mode? = null
        val colorAttrSet = false
        val colorAttr = 0
        val alpha = -1F


    }

    private fun tintDrawable(context: Context, @AttrRes resId: Int, drawable: Drawable, alpha: Int = -1) {
        val color = context.getThemeAttrColor(resId)

        if (alpha != -1) {
            drawable.alpha = alpha
        }
    }

    private fun setPorterDuffColorFilter(drawable: Drawable, color: Int, mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) {
        var filter = COLOR_FILTER_CACHE.get(color, mode)

        if (filter == null) {
            filter = PorterDuffColorFilter(color, mode)
            COLOR_FILTER_CACHE.put(color, mode, filter)
        }

        drawable.colorFilter = filter
    }

    private class ColorFilterLruCache(maxSize: Int) :
            LruCache<Int, PorterDuffColorFilter>(maxSize) {

        fun get(color: Int, mode: PorterDuff.Mode): PorterDuffColorFilter? {
            return get(generateCacheKey(color, mode))
        }

        fun put(color: Int, mode: PorterDuff.Mode,
                filter: PorterDuffColorFilter): PorterDuffColorFilter? {
            return put(generateCacheKey(color, mode), filter)
        }

        private fun generateCacheKey(color: Int, mode: PorterDuff.Mode): Int {
            var hashCode = 1
            hashCode = 31 * hashCode + color
            hashCode = 31 * hashCode + mode.hashCode()
            return hashCode
        }

    }

}
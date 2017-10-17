package com.example.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.annotation.AttrRes
import android.support.annotation.StringRes
import android.support.v4.graphics.ColorUtils
import android.util.TypedValue
import android.widget.Toast

private fun Context.getThemeAttrColor(@AttrRes resId: Int): Int {
    val attrs = intArrayOf(resId)
    val typedArray = obtainStyledAttributes(attrs)

    try {
        return typedArray.getColor(0, 0)
    } finally {
        typedArray.recycle()
    }
}

fun Context.getThemeAttrColorStateList(@AttrRes resId: Int): ColorStateList? {
    val attrs = intArrayOf(resId)
    val typedArray = obtainStyledAttributes(attrs)
    try {
        return typedArray.getColorStateList(0)
    } finally {
        typedArray.recycle()
    }
}

fun Context.getDisabledThemeAttrColor(@AttrRes resId: Int): Int {
    val colorStateList = getThemeAttrColorStateList(resId)

    if (colorStateList != null && colorStateList.isStateful) {
        return colorStateList.getColorForState(StateSet.DISABLED, colorStateList.defaultColor)
    } else {
        val typedValue = TypedValue()

        theme.resolveAttribute(android.R.attr.disabledAlpha, typedValue, true)
        val disabledAlpha = typedValue.float

        return getThemeAttrColor(resId, disabledAlpha)
    }
}

fun Context.getThemeAttrColor(@AttrRes resId: Int, alpha: Float = 1F): Int {
    val color = getThemeAttrColor(resId)

    if (alpha != 1F) {
        val originalAlpha = Color.alpha(color)
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha))
    } else {
        return color
    }
}

fun Context.toast(@StringRes resId: Int, longDuration: Boolean = false) {
    Toast.makeText(this, resId, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.toast(text: CharSequence, longDuration: Boolean = false) {
    Toast.makeText(this, text, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}
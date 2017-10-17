package com.example.app

object StateSet {

    val DISABLED = intArrayOf(-android.R.attr.state_enabled)

    val FOCUSED = intArrayOf(android.R.attr.state_focused)

    val ACTIVATED = intArrayOf(android.R.attr.state_activated)

    val PRESSED = intArrayOf(android.R.attr.state_pressed)

    val CHECKED = intArrayOf(android.R.attr.state_checked)

    val SELECTED = intArrayOf(android.R.attr.state_selected)

    val NOT_PRESSED_OR_FOCUSED = intArrayOf(
            -android.R.attr.state_pressed, -android.R.attr.state_focused)

    val ENABLED = intArrayOf()

    val DEFAULT = ENABLED

}
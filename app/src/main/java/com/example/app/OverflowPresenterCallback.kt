package com.example.app

import android.annotation.SuppressLint
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.view.menu.MenuPresenter
import android.support.v7.view.menu.SubMenuBuilder

@SuppressLint("RestrictedApi")
class OverflowPresenterCallback : MenuPresenter.Callback {

    var callback: MenuPresenter.Callback? = null

    override fun onCloseMenu(menu: MenuBuilder, allMenusAreClosing: Boolean) {
        if (menu is SubMenuBuilder) {
            menu.rootMenu.close(false)
        }

        callback?.onCloseMenu(menu, allMenusAreClosing)
    }

    override fun onOpenSubMenu(subMenu: MenuBuilder?): Boolean {
        if (subMenu == null) return false
        return callback?.onOpenSubMenu(subMenu) ?: false
    }

}
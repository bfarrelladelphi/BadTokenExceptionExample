package com.example.app

import android.animation.Animator
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Parcelable
import android.os.SystemClock
import android.support.annotation.AttrRes
import android.support.annotation.StyleRes
import android.support.v4.internal.view.SupportMenu
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v7.view.menu.*
import android.support.v7.widget.MenuItemHoverListener
import android.support.v7.widget.MenuPopupWindow
import android.view.*
import android.widget.*
import com.example.app.OverflowAdapter
import com.example.app.R
import java.util.*



@SuppressLint("RestrictedApi")
class OverflowPopup(
        private val context: Context,
        private var anchor: View,
        @AttrRes private val popupStyleAttribute: Int = R.attr.actionOverflowMenuStyle,
        @StyleRes private val popupStyleResource: Int = 0,
        private val overflowOnly: Boolean = true
) : ShowableListMenu, MenuPresenter, AdapterView.OnItemClickListener, View.OnKeyListener, PopupWindow.OnDismissListener {

    companion object {

        private const val HORIZONTAL_POSITION_LEFT = 0
        private const val HORIZONTAL_POSITION_RIGHT = 1

        private const val SUBMENU_TIMEOUT_MILLISECONDS = 200

    }

    private val menuMaxWidth = Math.max(context.resources.displayMetrics.widthPixels / 2,
            context.resources.getDimensionPixelSize(R.dimen.abc_config_prefDialogWidth))
    private val menuMinWidth = context.resources.getDimensionPixelSize(R.dimen.menu_width)
    private val subMenuHoverHandler = Handler()

    private val pendingMenus = LinkedList<MenuBuilder>()
    private val showingMenus = ArrayList<OverflowMenuInfo>()

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        if (isShowing && showingMenus.size > 0 && !showingMenus[0].window.isModal) {
            val anchor = shownAnchor
            if (anchor == null || !anchor.isShown) {
                dismiss()
            } else {
                for (info in showingMenus) {
                    info.window.show()
                }
            }
        }
    }

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {

        override fun onViewAttachedToWindow(view: View) {}

        override fun onViewDetachedFromWindow(view: View) {
            treeObserver?.let {
                if (!it.isAlive) {
                    treeObserver = view.viewTreeObserver
                }
                treeObserver?.removeGlobalOnLayoutListener(globalLayoutListener)
            }
            view.removeOnAttachStateChangeListener(this)
        }
    }

    private val menuItemHoverListener = object : MenuItemHoverListener {

        override fun onItemHoverExit(menu: MenuBuilder, item: MenuItem) {
            subMenuHoverHandler.removeCallbacksAndMessages(menu)
        }

        override fun onItemHoverEnter(menu: MenuBuilder, item: MenuItem) {
            subMenuHoverHandler.removeCallbacksAndMessages(null)

            val index = showingMenus.indexOfFirst { menu == it.menu }
            if (index == -1) return

            val nextIndex = index + 1

            val nextInfo = if (nextIndex < showingMenus.size) {
                showingMenus[nextIndex]
            } else {
                null
            }

            val runnable = Runnable {
                nextInfo?.let {
                    shouldCloseImmediately = true
                    it.menu.close(false)
                    shouldCloseImmediately = false
                }

                if (item.isEnabled && item.hasSubMenu()) {
                    menu.performItemAction(item, SupportMenu.FLAG_KEEP_OPEN_ON_SUBMENU_OPENED)
                }
            }

            val uptimeMilliseconds = SystemClock.uptimeMillis() + SUBMENU_TIMEOUT_MILLISECONDS
            subMenuHoverHandler.postAtTime(runnable, menu, uptimeMilliseconds)
        }

    }

    private var rawGravity = Gravity.NO_GRAVITY
    var gravity = Gravity.NO_GRAVITY
        set(value) {
            if (rawGravity != value) {
                rawGravity = value
                field = GravityCompat.getAbsoluteGravity(
                        value, ViewCompat.getLayoutDirection(anchor))
            }
        }

    var shownAnchor: View? = null

    private var lastPosition = getInitialMenuPosition()

    var horizontalOffset: Int? = null
    var verticalOffset: Int? = null

    private var presenterCallback: MenuPresenter.Callback? = null

    private var treeObserver: ViewTreeObserver? = null

    var onDismissListener: PopupWindow.OnDismissListener? = null

    private var shouldCloseImmediately = false

    var epicenterBounds = Rect()

    private var itemEnterAnimator: AnimatorSet? = null

    private fun createPopupWindow(): MenuPopupWindow {
        val popupWindow = MenuPopupWindow(
                context, null, popupStyleAttribute, popupStyleResource)
        popupWindow.setHoverListener(menuItemHoverListener)
        popupWindow.setOnItemClickListener(this)
        popupWindow.setOnDismissListener(this)
        popupWindow.anchorView = anchor
        popupWindow.isModal = true
        popupWindow.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        return popupWindow
    }

    override fun show() {
        if (isShowing) return

        for (menu in pendingMenus) showMenu(menu)
        pendingMenus.clear()

        shownAnchor = anchor
        shownAnchor?.let {
            val addGlobalListener = treeObserver == null
            treeObserver = it.viewTreeObserver

            if (addGlobalListener) {
                treeObserver?.addOnGlobalLayoutListener(globalLayoutListener)
            }

            it.addOnAttachStateChangeListener(attachStateChangeListener)
        }
    }

    override fun dismiss() {
        val length = showingMenus.size
        if (length > 0) {
            for (i in (length - 1) downTo 0) {
                val info = showingMenus[i]
                if (info.window.isShowing) info.window.dismiss()
            }
        }
    }

    override fun onKey(view: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        } else {
            return false
        }
    }

    private fun getInitialMenuPosition(): Int {
        return if (ViewCompat.getLayoutDirection(anchor) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            HORIZONTAL_POSITION_LEFT
        } else {
            HORIZONTAL_POSITION_RIGHT
        }
    }

    private fun getNextMenuPosition(nextMenuWidth: Int): Int {
        val lastListView = showingMenus.last().listView

        val screenLocation = IntArray(2)
        lastListView?.getLocationOnScreen(screenLocation)

        val displayFrame = Rect()
        shownAnchor?.getWindowVisibleDisplayFrame(displayFrame)

        if (lastPosition == HORIZONTAL_POSITION_RIGHT) {
            val right = screenLocation[0] + (lastListView?.width ?: 0) + nextMenuWidth
            return if (right > displayFrame.right) {
                HORIZONTAL_POSITION_LEFT
            } else {
                HORIZONTAL_POSITION_RIGHT
            }
        } else {
            val left = screenLocation[0] - nextMenuWidth
            return if (left < 0) {
                HORIZONTAL_POSITION_RIGHT
            } else {
                HORIZONTAL_POSITION_LEFT
            }
        }
    }

    fun addMenu(menu: MenuBuilder) {
        menu.addMenuPresenter(this, context)

        if (isShowing) {
            showMenu(menu)
        } else {
            pendingMenus.add(menu)
        }
    }

    override fun initForMenu(context: Context, menu: MenuBuilder) {
        // Do not need to do anything; added as a presenter in the constructor.
    }

    override fun getMenuView(root: ViewGroup): MenuView {
        throw UnsupportedOperationException("OverflowPopup manages its own views.")
    }

    override fun expandItemActionView(menu: MenuBuilder, item: MenuItemImpl): Boolean = false

    override fun collapseItemActionView(menu: MenuBuilder, item: MenuItemImpl): Boolean = false

    override fun getId(): Int = 0

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val outerAdapter = parent.adapter
        val wrappedAdapter = outerAdapter as MenuAdapter

        wrappedAdapter.adapterMenu.performItemAction(
                outerAdapter.getItem(position) as MenuItem, this,
                SupportMenu.FLAG_KEEP_OPEN_ON_SUBMENU_OPENED)
    }

    private fun measureIndividualMenuWidth(
            adapter: ListAdapter, parent: ViewGroup?, context: Context, maxAllowedWidth: Int
    ): Int {
        var maxWidth = menuMinWidth
        var itemView: View? = null
        var itemType = 0

        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        for (i in 0 until adapter.count) {
            val positionType = adapter.getItemViewType(i)

            if (positionType != itemType) {
                itemType = positionType
                itemView = null
            }

            itemView = adapter.getView(i, itemView, parent ?: FrameLayout(context))
            itemView.measure(widthMeasureSpec, heightMeasureSpec)

            val itemWidth = itemView.measuredWidth
            if (itemWidth >= maxAllowedWidth) {
                return maxAllowedWidth
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth
            }
        }

        return maxWidth
    }

    private fun showMenu(menu: MenuBuilder) {
        val inflater = LayoutInflater.from(context)
        val adapter = OverflowAdapter(menu, inflater, overflowOnly)

        val menuWidth = measureIndividualMenuWidth(adapter, null, context, menuMaxWidth)

        val popupWindow = createPopupWindow()
        popupWindow.setAdapter(adapter)
        popupWindow.setContentWidth(menuWidth)
        popupWindow.setDropDownGravity(gravity)

        val parentView = if (showingMenus.size > 0) {
            findParentViewForSubmenu(showingMenus.last(), menu)
        } else {
            null
        }

        if (parentView != null) {
            popupWindow.setTouchModal(false)
            popupWindow.setEnterTransition(null)

            val nextMenuPosition = getNextMenuPosition(menuWidth)
            val showOnRight = nextMenuPosition == HORIZONTAL_POSITION_RIGHT
            lastPosition = nextMenuPosition

            val parentOffsetLeft: Int
            val parentOffsetTop: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                popupWindow.anchorView = parentView
                parentOffsetLeft = 0
                parentOffsetTop = 0
            } else {
                val anchorScreenLocation = IntArray(2)
                anchor.getLocationOnScreen(anchorScreenLocation)

                val parentViewScreenLocation = IntArray(2)
                parentView.getLocationOnScreen(parentViewScreenLocation)

                parentOffsetLeft = parentViewScreenLocation[0] - anchorScreenLocation[0]
                parentOffsetTop = parentViewScreenLocation[0] - anchorScreenLocation[1]
            }

            val x = if ((gravity and Gravity.RIGHT) == Gravity.RIGHT) {
                if (showOnRight) {
                    parentOffsetLeft + menuWidth
                } else {
                    parentOffsetLeft - parentView.width
                }
            } else {
                if (showOnRight) {
                    parentOffsetLeft + parentView.width
                } else {
                    parentOffsetLeft - menuWidth
                }
            }

            popupWindow.horizontalOffset = x

            popupWindow.setOverlapAnchor(true)
            popupWindow.verticalOffset = parentOffsetTop
        } else {
            horizontalOffset?.let { popupWindow.horizontalOffset = it }
            verticalOffset?.let { popupWindow.verticalOffset = it }

            popupWindow.setEpicenterBounds(epicenterBounds)
        }

        val menuInfo = OverflowMenuInfo(popupWindow, menu, lastPosition)
        showingMenus.add(menuInfo)

        popupWindow.show()

        val listView = popupWindow.listView
        listView?.setOnKeyListener(this)
        listView?.addOnLayoutChangeListener(object : View.OnLayoutChangeListener{
            override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                listView.removeOnLayoutChangeListener(this)
                runMenuItemEnterAnimations(listView)
            }
        })
    }

    private fun runMenuItemEnterAnimations(listView: ListView) {
        itemEnterAnimator = AnimatorSet()
        var builder: AnimatorSet.Builder? = null

        for (i in 0 until listView.childCount) {
            val view = listView.getChildAt(i)
            val animatorObject = view.getTag(R.id.menuItemEnterAnimation)
            if (animatorObject != null) {
                if (builder == null) {
                    builder = itemEnterAnimator?.play(animatorObject as Animator)
                } else {
                    builder.with(animatorObject as Animator)
                }
            }
        }

        itemEnterAnimator?.start()
    }

    private fun findMenuItemForSubmenu(parent: MenuBuilder, submenu: MenuBuilder): MenuItem? {
        for (i in 0..parent.size()) {
            val item = parent.getItem(i)
            if (item.hasSubMenu() && submenu == item.subMenu) {
                return item
            }
        }

        return null
    }

    private fun findParentViewForSubmenu(parentInfo: OverflowMenuInfo, submenu: MenuBuilder): View? {
        val owner = findMenuItemForSubmenu(parentInfo.menu, submenu) ?: return null

        val headersCount: Int
        val menuAdapter: MenuAdapter?

        val listAdapter = parentInfo.listView?.adapter

        if (listAdapter is HeaderViewListAdapter) {
            headersCount = listAdapter.headersCount
            menuAdapter = listAdapter.wrappedAdapter as MenuAdapter?
        } else {
            headersCount = 0
            menuAdapter = listAdapter as MenuAdapter?
        }

        var ownerPosition = (0..(menuAdapter?.count ?: 0)).indexOfFirst { owner == menuAdapter?.getItem(it) }

        if (ownerPosition == ListView.INVALID_POSITION) return null

        ownerPosition += headersCount

        val ownerViewPosition = ownerPosition - (listView?.firstVisiblePosition ?: 0)
        if (ownerViewPosition < 0 || ownerViewPosition >= (listView?.childCount ?: -1)) return null

        return listView?.getChildAt(ownerViewPosition)
    }

    override fun isShowing(): Boolean = showingMenus.size > 0 && showingMenus[0].window.isShowing

    override fun onDismiss() {
        itemEnterAnimator?.cancel()
        itemEnterAnimator = null
        showingMenus.firstOrNull { !it.window.isShowing }?.menu?.close(false)
    }

    override fun updateMenuView(cleared: Boolean) {
        for (info in showingMenus) {
            info.listView?.let {
                (it.adapter as MenuAdapter).notifyDataSetChanged()
            }
        }
    }

    override fun setCallback(callback: MenuPresenter.Callback?) { presenterCallback = callback }

    override fun onSubMenuSelected(subMenu: SubMenuBuilder): Boolean {
        for (info in showingMenus) {
            if (subMenu == info.menu) {
                info.listView?.requestFocus()
                return true
            }
        }

        return if (subMenu.hasVisibleItems()) {
            addMenu(subMenu)
            presenterCallback?.onOpenSubMenu(subMenu)
            true
        } else {
            false
        }
    }

    override fun onCloseMenu(menu: MenuBuilder?, allMenusAreClosing: Boolean) {
        val index = showingMenus.indexOfFirst { menu == it.menu }
        if (index < 0) return

        val nextIndex = index + 1
        if (nextIndex < showingMenus.size) {
            showingMenus[nextIndex].menu.close(false)
        }

        val info = showingMenus.removeAt(index)
        info.menu.removeMenuPresenter(this)

        info.window.dismiss()

        lastPosition = if (showingMenus.size > 0) {
            showingMenus[showingMenus.size - 1].position
        } else {
            getInitialMenuPosition()
        }

        if (showingMenus.size == 0) {
            dismiss()
            presenterCallback?.onCloseMenu(menu, true)

            treeObserver?.let {
                if (it.isAlive) {
                    it.removeGlobalOnLayoutListener(globalLayoutListener)
                }
                treeObserver = null
            }

            shownAnchor?.removeOnAttachStateChangeListener(attachStateChangeListener)

            onDismissListener?.onDismiss()
        } else if (allMenusAreClosing) {
            showingMenus[0].menu.close(false)
        }
    }

    override fun flagActionItems(): Boolean = false

    override fun onSaveInstanceState(): Parcelable? = null

    override fun onRestoreInstanceState(state: Parcelable?) {}

    override fun getListView(): ListView? = if (showingMenus.isEmpty()) {
        null
    } else {
        showingMenus.last().listView
    }

    private inner class OverflowMenuInfo(
            val window: MenuPopupWindow,
            val menu: MenuBuilder,
            val position: Int
    ) {

        val listView = window.listView

    }

}
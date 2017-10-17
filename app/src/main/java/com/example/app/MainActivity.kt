package com.example.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.widget.TooltipCompat

import kotlinx.android.synthetic.main.activity_main.*

@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val menu = MenuBuilder(this)
        menuInflater.inflate(R.menu.options, menu)

        val popupHelper = OverflowPopupHelper(this, menu, menuButton, true)
        menuButton.setOnClickListener { popupHelper.show() }

        TooltipCompat.setTooltipText(menuButton, getText(R.string.desc_menu_button))
    }

}
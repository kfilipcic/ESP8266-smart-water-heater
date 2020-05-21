package com.example.rijekasmarthomeapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity


class ACRemoteDialog : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
        val factor: Int = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 500)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        setContentView(R.layout.ac_remote_dialog)

        /*
        val root: RelativeLayout = findViewById(R.id.relativeLayoutACDialog)
        val ib: ImageButton = findViewById(R.id.powerButtonAC)

        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(150, 150)
        params.leftMargin = 50
        params.topMargin = 900
        root.removeView(ib)
        root.addView(ib, params)*/

    }
}
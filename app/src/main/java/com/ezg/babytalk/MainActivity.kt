package com.ezg.babytalk

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        val title = TextView(this).apply { text = "EZG BABY TALK"; textSize = 28f; gravity = Gravity.CENTER }
        val subtitle = TextView(this).apply { text = "Bebek ve ebeveyn cihazını eşleştir"; textSize = 16f; gravity = Gravity.CENTER; setPadding(0, 18, 0, 28) }
        val baby = Button(this).apply { text = "BU TELEFON BEBEK YANINDA"; isAllCaps = false }
        val parent = Button(this).apply { text = "EBEVEYN OLARAK DİNLE"; isAllCaps = false }
        val status = TextView(this).apply { text = "Henüz bağlanmadı • İlk sürüm arayüzü"; textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 0) }
        baby.setOnClickListener { status.text = "Bebek cihazı seçildi. Canlı ses bağlantısı henüz etkin değil." }
        parent.setOnClickListener { status.text = "Ebeveyn cihazı seçildi. Canlı ses bağlantısı henüz etkin değil." }
        root.addView(title); root.addView(subtitle); root.addView(baby); root.addView(parent); root.addView(status)
        setContentView(root)
    }
}

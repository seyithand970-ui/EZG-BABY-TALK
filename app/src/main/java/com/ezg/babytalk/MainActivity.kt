package com.ezg.babytalk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.security.SecureRandom

class MainActivity : Activity() {
    private lateinit var code: EditText
    private lateinit var status: TextView
    private var role = "baby"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = 1; gravity = Gravity.CENTER; setPadding(28,28,28,28) }
        layout.addView(TextView(this).apply { text = "EZG BABY TALK"; textSize = 28f; gravity = Gravity.CENTER })
        layout.addView(TextView(this).apply { text = "İki telefonu oda koduyla eşleştir"; textSize = 16f; gravity = Gravity.CENTER; setPadding(0,16,0,20) })
        code = EditText(this).apply { hint = "6 haneli oda kodu"; textSize = 22f; gravity = Gravity.CENTER; inputType = 2; filters = arrayOf(android.text.InputFilter.LengthFilter(6)) }
        status = TextView(this).apply { text = "Bebek telefonunda kod oluştur; ebeveyn telefonuna aynı kodu gir."; gravity = Gravity.CENTER; setPadding(0,18,0,0) }
        layout.addView(code)
        layout.addView(Button(this).apply { text = "BU TELEFON BEBEK YANINDA"; setOnClickListener { role="baby"; code.setText((100000+SecureRandom().nextInt(900000)).toString()); startMonitor() } })
        layout.addView(Button(this).apply { text = "EBEVEYN OLARAK DİNLE"; setOnClickListener { role="parent"; if(code.text.length!=6) code.error="6 haneli kodu gir" else startMonitor() } })
        layout.addView(Button(this).apply { text = "BAĞLANTIYI DURDUR"; setOnClickListener { stopService(Intent(this@MainActivity, MonitorService::class.java)); status.text="Bağlantı durduruldu." } })
        layout.addView(status)
        setContentView(layout)
    }
    private fun startMonitor() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) { requestPermissions(missing.toTypedArray(), 41); return }
        launch()
    }
    private fun launch() {
        val i=Intent(this,MonitorService::class.java).putExtra("role",role).putExtra("room",code.text.toString())
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i)
        status.text=if(role=="baby") "Bebek cihazı başlatıldı. Ebeveyn telefonuna bu kodu gir: ${code.text}" else "Ebeveyn cihazı başlatıldı. Bebek telefonu bekleniyor."
    }
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==41 && grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED) launch()
        else status.text="Mikrofon izni gerekli."
    }
}

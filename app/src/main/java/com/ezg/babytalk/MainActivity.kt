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
import android.widget.Toast
import java.security.SecureRandom

class MainActivity : Activity() {
    private lateinit var room: EditText
    private lateinit var status: TextView
    private var selectedRole = "baby"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(28, 28, 28, 28) }
        val title = TextView(this).apply { text = "EZG BABY TALK"; textSize = 28f; gravity = Gravity.CENTER }
        val subtitle = TextView(this).apply { text = "İki telefonu oda koduyla eşleştir"; textSize = 16f; gravity = Gravity.CENTER; setPadding(0, 16, 0, 22) }
        room = EditText(this).apply { hint = "6 haneli oda kodu"; textSize = 20f; gravity = Gravity.CENTER; inputType = 2; setSingleLine(true); filters = arrayOf(android.text.InputFilter.LengthFilter(6)) }
        val baby = Button(this).apply { text = "BU TELEFON BEBEK YANINDA"; isAllCaps = false }
        val parent = Button(this).apply { text = "EBEVEYN OLARAK DİNLE"; isAllCaps = false }
        val stop = Button(this).apply { text = "BAĞLANTIYI DURDUR"; isAllCaps = false }
        status = TextView(this).apply { text = "Bebek telefonunda kod oluştur; ebeveyn telefonuna aynı kodu gir."; textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 18, 0, 0) }
        baby.setOnClickListener { selectedRole = "baby"; val code = (100000 + SecureRandom().nextInt(900000)).toString(); room.setText(code); startMonitor() }
        parent.setOnClickListener { selectedRole = "parent"; if (room.text.toString().length != 6) { room.error = "6 haneli kodu gir" } else startMonitor() }
        stop.setOnClickListener { stopService(Intent(this, MonitorService::class.java)); status.text = "Bağlantı durduruldu." }
        root.addView(title); root.addView(subtitle); root.addView(room); root.addView(baby); root.addView(parent); root.addView(stop); root.addView(status)
        setContentView(root)
    }

    private fun startMonitor() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) { requestPermissions(missing.toTypedArray(), 41); return }
        launchService()
    }

    private fun launchService() {
        val code = room.text.toString()
        val intent = Intent(this, MonitorService::class.java).putExtra("role", selectedRole).putExtra("room", code)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        status.text = if (selectedRole == "baby") "Bebek cihazı başlatılıyor. Ebeveyn telefonuna $code kodunu gir." else "Ebeveyn cihazı başlatılıyor. Bağlantı bekleniyor."
        Toast.makeText(this, "EZG BABY TALK başlatıldı", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) launchService()
        else status.text = "Mikrofon izni olmadan ses aktarımı yapılamaz."
    }
}

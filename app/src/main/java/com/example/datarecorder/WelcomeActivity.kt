package com.example.datarecorder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var checkBoxAgree: CheckBox
    private lateinit var btnAgree: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val agreed = prefs.getBoolean("disclaimer_agreed", false)

        if (agreed) {
            goMain()
            return
        }

        setContentView(R.layout.activity_welcome)

        checkBoxAgree = findViewById(R.id.checkBoxAgree)
        btnAgree = findViewById(R.id.btnAgree)

        btnAgree.isEnabled = false
        btnAgree.alpha = 0.5f

        checkBoxAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAgree.isEnabled = isChecked
            btnAgree.alpha = if (isChecked) 1f else 0.5f
        }

        btnAgree.setOnClickListener {
            if (!checkBoxAgree.isChecked) return@setOnClickListener

            prefs.edit()
                .putBoolean("disclaimer_agreed", true)
                .apply()

            goMain()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val agreedNow = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getBoolean("disclaimer_agreed", false)
                if (agreedNow) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

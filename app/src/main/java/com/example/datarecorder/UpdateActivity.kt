package com.example.datarecorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UpdateActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvContent: TextView
    private lateinit var btnUpdate: Button
    private lateinit var btnLater: Button

    private var forceUpdate: Boolean = false
    private var downloadUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        tvTitle = findViewById(R.id.tvTitle)
        tvVersion = findViewById(R.id.tvVersion)
        tvContent = findViewById(R.id.tvContent)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnLater = findViewById(R.id.btnLater)

        val title = intent.getStringExtra("updateTitle").orEmpty()
        val versionName = intent.getStringExtra("versionName").orEmpty()
        val content = intent.getStringExtra("updateContent").orEmpty()
        downloadUrl = intent.getStringExtra("downloadUrl").orEmpty()
        forceUpdate = intent.getBooleanExtra("forceUpdate", false)

        tvTitle.text = if (title.isBlank()) "发现新版本" else title
        tvVersion.text = "最新版本：$versionName"
        tvContent.text = if (content.isBlank()) "请更新到最新版本" else content

        btnLater.visibility = if (forceUpdate) Button.GONE else Button.VISIBLE

        btnUpdate.setOnClickListener {
            if (downloadUrl.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intent)
            }
        }

        btnLater.setOnClickListener {
            goNextPage()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!forceUpdate) {
            goNextPage()
        }
    }

    private fun goNextPage() {
        val token = getSharedPreferences("user_info", MODE_PRIVATE)
            .getString("token", "")

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}

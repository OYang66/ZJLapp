package com.example.datarecorder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.model.LoginRequest
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoRegister = findViewById(R.id.btnGoRegister)

        val logoutReason = SessionManager.consumeLogoutReason(this)
        if (logoutReason.isNotBlank()) {
            Toast.makeText(this, logoutReason, Toast.LENGTH_LONG).show()
        }

        btnLogin.setOnClickListener {
            doLogin()
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun doLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(
                    LoginRequest(
                        username = username,
                        password = password
                    )
                )

                if (response.code == 200 && response.data != null) {
                    val data = response.data
                    SessionManager.saveLogin(
                        this@LoginActivity,
                        data.token,
                        data.username,
                        data.userId
                    )
                    AccountStatusScheduler.start(this@LoginActivity)

                    Toast.makeText(
                        this@LoginActivity,
                        response.message.ifBlank { "登录成功" },
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        response.message.ifBlank { "登录失败" },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "网络异常，请稍后重试",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

package com.example.datarecorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.model.RegisterRequest
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoLogin: Button
    private lateinit var btnViewTutorial: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        RetrofitClient.init(applicationContext)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoLogin = findViewById(R.id.btnGoLogin)
        btnViewTutorial = findViewById(R.id.btnViewTutorial)

        btnRegister.setOnClickListener {
            doRegister()
        }

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnViewTutorial.setOnClickListener {
            openTutorial()
        }
    }

    private fun doRegister() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "请输入真实姓名", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 2) {
            Toast.makeText(this, "姓名至少2位", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val checkResponse = RetrofitClient.api.checkRegisterAccount(username)
                if (checkResponse.code != 200) {
                    Toast.makeText(
                        this@RegisterActivity,
                        checkResponse.message ?: "该账号不允许注册",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val registerResponse = RetrofitClient.api.register(
                    RegisterRequest(
                        username = username,
                        password = password
                    )
                )

                if (registerResponse.code == 200) {
                    Toast.makeText(
                        this@RegisterActivity,
                        registerResponse.message ?: "注册成功",
                        Toast.LENGTH_SHORT
                    ).show()

                    SessionManager.saveRememberedAccount(
                        this@RegisterActivity,
                        username,
                        password
                    )

                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        registerResponse.message ?: "注册失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "网络异常：" + (e.message ?: "请稍后重试"),
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun openTutorial() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yxff.work/"))
        startActivity(intent)
    }
}

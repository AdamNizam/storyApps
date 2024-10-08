package com.example.storyapps.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storyapps.databinding.ActivityLoginBinding
import com.example.storyapps.di.Injection
import com.example.storyapps.ui.register.RegisterActivity
import com.example.storyapps.ui.story.StoryActivity
import com.google.android.material.textfield.TextInputLayout
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(Injection.provideUserLoginRepository(this))
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.loginActivity) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.loginButton.isEnabled = false
        setupLoginFormValidation()
        binding.registerTextView.setOnClickListener { navigateTo(RegisterActivity::class.java) }
        binding.loginButton.setOnClickListener { loginUser() }
    }

    private fun setupLoginFormValidation() {
        val emailEditText = binding.emailEditText
        val passwordEditText = binding.passwordEditText
        val emailInputLayout = binding.emailInputLayout
        val passwordInputLayout = binding.passwordInputLayout

        addTextWatcher(emailEditText, emailInputLayout, "Email harus mengandung @gmail.com") {
            it.contains("@gmail.com")
        }
        addTextWatcher(passwordEditText, passwordInputLayout, "Password minimal 8 karakter") {
            it.length >= 8
        }
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onLoginInputChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onLoginInputChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun addTextWatcher(
        editText: EditText,
        inputLayout: TextInputLayout,
        errorMessage: String,
        validation: (String) -> Boolean
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString()
                if (!validation(inputText)) {
                    inputLayout.error = errorMessage
                } else {
                    inputLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loginUser(){
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailEditText.error = " Email Anda Salah"
            return
        }
        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password Anda Salah"
            return
        }

        binding.loadingProgressBar.visibility = View.VISIBLE
        loginViewModel.login(email, password).observe(this)
        { result ->
            result.onSuccess { response ->
                val token = response.loginResult.token
                saveToken(token)
                val intent = Intent(this, StoryActivity::class.java)
                Toast.makeText(this, "Login Succesfully", Toast.LENGTH_LONG).show()
                startActivity(intent)
                finish()
            }
            result.onFailure { throwable ->
                binding.loadingProgressBar.visibility = View.GONE

                Toast.makeText(this, throwable.message, Toast.LENGTH_SHORT).show()

                Log.e("LoginActivity", "Login gagal: ${throwable.message}")

                if (throwable is HttpException) {
                    val errorBody = throwable.response()?.errorBody()?.string()
                    Log.e("LoginActivity", "Response body: $errorBody")
                }
            }
        }

    }

    private fun onLoginInputChanged() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val isEmailValid = email.contains("@gmail.com")
        val isPasswordValid = password.length >= 8
        binding.loginButton.isEnabled = isEmailValid && isPasswordValid
    }

    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("auth_token", token)
        editor.apply()
    }

    private fun <T> Activity.navigateTo(targetActivity: Class<T>) {
        val intent = Intent(this, targetActivity)
        startActivity(intent)
    }

}
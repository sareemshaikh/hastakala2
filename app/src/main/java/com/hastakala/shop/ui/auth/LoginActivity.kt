package com.hastakala.testshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hastakala.testshop.databinding.ActivityLoginBinding
import com.hastakala.testshop.R
import com.hastakala.testshop.ui.MainActivity
import com.hastakala.testshop.viewmodel.AuthState
import com.hastakala.testshop.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Skip login screen if already authenticated
        if (viewModel.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            when {
                email.isBlank() -> showMessage("Email is required")
                password.isBlank() -> showMessage("Password is required")
                else -> viewModel.login(email, password)
            }
        }

        binding.btnGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Forgot password: read email field and send reset
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            viewModel.forgotPassword(email)
        }
        
        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                    }
                    is AuthState.Authenticated -> navigateToMain()
                    is AuthState.PasswordResetSent -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        showMessage("Password reset email sent. Check your inbox.")
                        viewModel.resetState()
                    }
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        showMessage(state.message)
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.coordinatorRoot, message, Snackbar.LENGTH_LONG).show()
    }
}

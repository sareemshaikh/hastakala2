package com.hastakala.testshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hastakala.testshop.databinding.ActivitySignupBinding
import com.hastakala.testshop.R
import com.hastakala.testshop.ui.MainActivity
import com.hastakala.testshop.viewmodel.AuthState
import com.hastakala.testshop.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            when {
                email.isBlank() -> showMessage("Email is required")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    showMessage("Enter a valid email address")
                password.length < 6 -> showMessage("Password must be at least 6 characters")
                else -> viewModel.signUp(email, password)
            }
        }

        binding.btnGoLogin.setOnClickListener {
            finish() // back to LoginActivity
        }
        
        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnSignup.isEnabled = false
                    }
                    is AuthState.Authenticated -> navigateToMain()
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSignup.isEnabled = true
                        showMessage(state.message)
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSignup.isEnabled = true
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

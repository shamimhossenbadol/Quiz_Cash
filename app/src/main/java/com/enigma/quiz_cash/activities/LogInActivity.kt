package com.enigma.quiz_cash.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth

class LogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding
    private var email: String? = null
    private var password: String? = null
    private var firebaseAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        firebaseAuth = FirebaseAuth.getInstance()
        binding.signUp.setOnClickListener {
            startActivity(Intent(this@LogInActivity, SignUpActivity::class.java))
            overridePendingTransition(R.anim.from_left, R.anim.to_right)
            this@LogInActivity.finish()
        }
        binding.forget.setOnClickListener {
            email = binding.email.text.toString()
            val emailPattern = "[a-zA-Z0-9._-]+@gmail.com"
            if (!email!!.matches(emailPattern.toRegex())) {
                binding.email.error = "Enter a valid email address"
            } else {
                firebaseAuth!!.sendPasswordResetEmail(email!!).addOnCompleteListener {
                    if (it.isSuccessful)
                        Toast.makeText(
                            this@LogInActivity,
                            "An email to reset your password has been sent to your email address.",
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        Toast.makeText(
                            this@LogInActivity,
                            it.exception?.localizedMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        }
        if (firebaseAuth!!.currentUser != null) {
            startActivity(Intent(this@LogInActivity, MainActivity::class.java))
            overridePendingTransition(R.anim.from_right, R.anim.to_left)
            this@LogInActivity.finish()
        }
        binding.logIn.setOnClickListener {
            email = binding.email.text.toString()
            password = binding.password.text.toString()
            val emailPattern = "[a-zA-Z0-9._-]+@gmail.com"
            if (!email!!.matches(emailPattern.toRegex())) {
                binding.email.error = "Enter a valid email address"
            } else if (password!!.isEmpty() || password!!.length < 8) {
                binding.password.error = "Enter valid password"
            } else {
                binding.progress.visibility = View.VISIBLE
                firebaseAuth!!.signInWithEmailAndPassword(email!!, password!!)
                    .addOnCompleteListener {
                        binding.progress.visibility = View.INVISIBLE
                        if (it.isSuccessful) {
                            startActivity(Intent(this@LogInActivity, MainActivity::class.java))
                            overridePendingTransition(R.anim.from_right, R.anim.to_left)
                            this@LogInActivity.finish()
                        } else {
                            Toast.makeText(
                                this@LogInActivity,
                                it.exception?.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
}
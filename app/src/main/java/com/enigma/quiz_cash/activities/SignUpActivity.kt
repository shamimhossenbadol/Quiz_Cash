package com.enigma.quiz_cash.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.databinding.ActivitySignUpBinding
import com.enigma.quiz_cash.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null
    private var name: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        binding.logIn.setOnClickListener {
            startActivity(Intent(this@SignUpActivity, LogInActivity::class.java))
            overridePendingTransition(R.anim.from_right, R.anim.to_left)
            this@SignUpActivity.finish()
        }
        binding.signUp.setOnClickListener { startSignUpProcess() }
    }

    private fun startSignUpProcess() {
        name = binding.name.text.toString()
        email = binding.email.text.toString()
        val password = binding.password.text.toString()
        val confirmPassword = binding.passwordConfirm.text.toString()
        val emailPattern = "[a-zA-Z0-9._-]+@gmail.com"
        if (name!!.isEmpty()) {
            binding.name.error = "Enter your name"
        } else if (!email!!.matches(emailPattern.toRegex())) {
            binding.email.error = "Enter a valid gmail address"
        } else if (password.isEmpty() || password.length < 8) {
            binding.password.error = "Enter valid password"
        } else if (confirmPassword != password) {
            binding.passwordConfirm.error = "Password not matched"
        } else {
            binding.progress.visibility = View.VISIBLE
            firebaseAuth!!.createUserWithEmailAndPassword(email!!, password)
                .addOnCompleteListener { task: Task<AuthResult?> ->
                    if (task.isSuccessful) {
                        storeUserData(name!!, email!!, generateReferCode())
                    } else {
                        Toast.makeText(
                            this@SignUpActivity,
                            task.exception?.localizedMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.progress.visibility = View.INVISIBLE
                    }
                }
        }
    }

    private fun storeUserData(name: String, email: String, referCode: String) {
        val user = User(name, email, 100, 0, referCode, 0, null, getTodayDate())
        firebaseAuth?.currentUser?.let { firebaseFirestore?.collection("users")?.document(it.uid) }
            ?.set(user)?.addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this@SignUpActivity, "Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUpActivity, LogInActivity::class.java))
                    overridePendingTransition(R.anim.from_right, R.anim.to_left)
                    this@SignUpActivity.finish()
                } else {
                    Toast.makeText(this@SignUpActivity, "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                    firebaseAuth!!.currentUser?.delete()?.addOnFailureListener {
                        firebaseAuth!!.currentUser?.delete()
                    }
                    firebaseAuth!!.signOut()
                }
                binding.progress.visibility = View.INVISIBLE
            }
    }

    private fun generateReferCode(): String {
        val aToZ = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = Random(System.currentTimeMillis())
        val sb = StringBuilder()
        for (i in 0 until 10) {
            val randIndex = random.nextInt(aToZ.length)
            sb.append(aToZ[randIndex])
        }
        return sb.toString()
    }

    private fun getTodayDate(): String? {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
        return dateFormat.format(currentDate)
    }
}

package com.enigma.quiz_cash.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.enigma.quiz_cash.CategoryClicked
import com.enigma.quiz_cash.DatabaseHelper
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.Utils
import com.enigma.quiz_cash.adapters.CategoryAdapter
import com.enigma.quiz_cash.databinding.ActivityMainBinding
import com.enigma.quiz_cash.databinding.RulesBinding
import com.enigma.quiz_cash.models.CategoryModel
import com.enigma.quiz_cash.models.User
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), CategoryClicked {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adRequest: AdRequest
    private lateinit var mAdView: AdView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private var user: User? = null
    private var utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setLogo(R.drawable.baseline_quiz_24)
        supportActionBar?.title = "  Quiz Cash"
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        mAdView = AdView(this)
        mAdView.adUnitId = getString(R.string.bannerAds)
        mAdView.setAdSize(getAdSize())
        binding.adview.addView(mAdView)
        mAdView.loadAd(adRequest)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        fetchUserDataFromFirebase()

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        val databaseHelper = DatabaseHelper.getInstance(this)
        databaseHelper.getDatabaseDao().getAllCategory().observe(this) {
            binding.recyclerView.adapter = CategoryAdapter(this, it, this)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Do you want to exit?")
                builder.setMessage("If you enjoy using this app, please take a moment to rate it and write a review on the Play Store. Your feedback helps us to improve the app's stability and make it even better for everyone.")
                builder.setPositiveButton("Yes") { _, _ ->
                    this@MainActivity.finish()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.setCancelable(false)
                val alertDialog = builder.create()
                alertDialog.window?.setWindowAnimations(R.style.AnimationPopup)
                alertDialog.show()
            }
        })
    }

    private fun fetchUserDataFromFirebase() {
        firebaseAuth.currentUser?.let { firebaseFirestore.collection("users").document(it.uid) }
            ?.get()?.addOnSuccessListener {
                user = it.toObject(User::class.java)
            }?.addOnFailureListener {
                val snackBar = Snackbar.make(
                    binding.root,
                    "Failed to load data",
                    Snackbar.LENGTH_INDEFINITE
                )
                snackBar.setAction("Reload") {
                    fetchUserDataFromFirebase()
                }
                snackBar.show()
            }
    }

    override fun onResume() {
        fetchUserDataFromFirebase()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.info) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setCancelable(true)
            val view = RulesBinding.inflate(layoutInflater)
            builder.setView(view.root)
            val dialog = builder.create()
            dialog.window?.setWindowAnimations(R.style.AnimationPopup)
            dialog.show()
        }
        if (!utils.isInternetConnected(this@MainActivity)) {
            utils.showToast(this@MainActivity, "No Internet Connection")
            return true
        }
        if (item.itemId == R.id.withdraw && user?.coins != null) {
            val intent = Intent(this@MainActivity, WithdrawActivity::class.java)
            intent.putExtra("name", user?.name)
            intent.putExtra("email", user?.email)
            intent.putExtra("coins", user?.coins)
            startActivity(intent)
            overridePendingTransition(R.anim.from_right, R.anim.to_left)
        }
        if (item.itemId == R.id.refer && user?.coins != null) {
            val intent = Intent(this@MainActivity, ReferActivity::class.java)
            intent.putExtra("coins", user?.coins)
            intent.putExtra("referCode", user?.referCode)
            intent.putExtra("referredBy", user?.referredBy)
            startActivity(intent)
            overridePendingTransition(R.anim.from_right, R.anim.to_left)
        }
        if (item.itemId == R.id.logout) {
            if (firebaseAuth.currentUser != null) {
                firebaseAuth.signOut()
                val intent = Intent(this@MainActivity, LogInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.from_left, R.anim.to_right)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    override fun onClicked(category: CategoryModel, color: String) {
        if (utils.isInternetConnected(this)) {
            val intent = Intent(this@MainActivity, QuizActivity::class.java)
            intent.putExtra("title", category.title)
            intent.putExtra("icon", category.icon)
            intent.putExtra("color", color)
            intent.putExtra("coins", user?.coins)
            startActivity(intent)
            overridePendingTransition(R.anim.from_right, R.anim.to_left)
        } else utils.showToast(this@MainActivity, "No Internet Connection")
    }
}
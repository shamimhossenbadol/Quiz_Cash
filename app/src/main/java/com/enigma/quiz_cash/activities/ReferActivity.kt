package com.enigma.quiz_cash.activities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.Utils
import com.enigma.quiz_cash.databinding.ActivityReferBinding
import com.enigma.quiz_cash.databinding.CoinsBoardBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ReferActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReferBinding
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var mAdView: AdView
    private lateinit var adRequest: AdRequest
    private lateinit var utils: Utils
    private var coins = 0
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private var rewardedAd: RewardedAd? = null
    private var isRewarded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        mAdView = AdView(this)
        mAdView.adUnitId = getString(R.string.bannerAds)
        mAdView.setAdSize(getAdSize())
        binding.adview.addView(mAdView)
        mAdView.loadAd(adRequest)
        loadInterstitialAds()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this@ReferActivity)
                } else {
                    this@ReferActivity.finish()
                    overridePendingTransition(R.anim.from_left, R.anim.to_right)
                }
            }
        })

        coins = intent.getIntExtra("coins", 0)
        val referCode = intent.getStringExtra("referCode")
        val referredBy = intent.getStringExtra("referredBy")
        if (referredBy.isNullOrBlank()) binding.referArea.visibility = View.VISIBLE
        else binding.referArea.visibility = View.GONE
        binding.coins.text = coins.toString()
        binding.referCode.text = referCode
        utils = Utils()
        binding.copyButton.setOnClickListener {
            val clipboardManager =
                this@ReferActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", referCode.toString())
            clipboardManager.setPrimaryClip(clipData)
            utils.showToast(this@ReferActivity, "Text copied successful")
        }
        binding.submitButton.setOnClickListener {
            if (utils.isInternetConnected(this@ReferActivity)) {
                val redeemCode = binding.redeemField.text.toString()
                if (redeemCode == "" || redeemCode.length < 10 || redeemCode == referCode) {
                    utils.showToast(this@ReferActivity, "Enter a valid refer code")
                } else {
                    loadRewardedAds(redeemCode)
                }
            } else utils.showToast(this@ReferActivity, "No Internet Connection")
        }

    }

    private fun redeemCodeAndAddBalance(redeemCode: String) {
        utils.showToast(this@ReferActivity, "Refer code verifying...")
        firebaseFirestore.collection("users").whereEqualTo("referCode", redeemCode).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result.isEmpty) {
                        utils.showToast(
                            this@ReferActivity,
                            "Refer code not found. Make sure you entered the right code."
                        )
                    } else {
                        firebaseAuth.uid?.let {
                            firebaseFirestore.collection("users").document(it)
                                .update(
                                    "referredBy", redeemCode,
                                    "coins", FieldValue.increment(100)
                                )
                                .addOnCompleteListener { task1 ->
                                    if (task1.isSuccessful) {
                                        binding.referArea.visibility = View.GONE
                                        firebaseFirestore.collection("users")
                                            .document(task.result.documents[0].id)
                                            .update(
                                                "coins",
                                                FieldValue.increment(100),
                                                "referCount",
                                                FieldValue.increment(1)
                                            )
                                        showCoinsDialog()
                                    } else {
                                        task1.exception?.localizedMessage?.let { it1 ->
                                            utils.showToast(this@ReferActivity, it1)
                                        }
                                    }
                                }
                        }
                    }
                } else {
                    task.exception?.localizedMessage?.let {
                        utils.showToast(this@ReferActivity, it)
                    }
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun showCoinsDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@ReferActivity)
        builder.setCancelable(false)
        val view = CoinsBoardBinding.inflate(layoutInflater)
        builder.setView(view.root)
        view.coinsView.text = "+100"
        val alertDialog: AlertDialog = builder.create()
        alertDialog.window?.setWindowAnimations(R.style.AnimationPopup)
        view.collectCoins.setOnClickListener {
            coins += 100
            binding.coins.text = coins.toString()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun getAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun loadInterstitialAds() {
        InterstitialAd.load(
            this,
            getString(R.string.interstitialAds),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                mInterstitialAd = null
                                this@ReferActivity.finish()
                                overridePendingTransition(R.anim.from_left, R.anim.to_right)
                            }
                        }
                }
            })
    }

    private fun loadRewardedAds(redeemCode: String) {
        utils.showToast(this@ReferActivity, "Ads loading...")
        RewardedAd.load(this, getString(R.string.rewardedAds), adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    utils.showToast(this@ReferActivity, "No ads found")
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            if (isRewarded) {
                                redeemCodeAndAddBalance(redeemCode)
                            }
                        }
                    }
                    rewardedAd!!.show(this@ReferActivity) { isRewarded = true }
                }
            })
    }
}
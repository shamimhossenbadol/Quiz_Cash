package com.enigma.quiz_cash.activities

import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.Utils
import com.enigma.quiz_cash.databinding.ActivityWithdrawBinding
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

class WithdrawActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWithdrawBinding
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var mAdView: AdView
    private lateinit var adRequest: AdRequest
    private var type = "Paypal"
    private var utils = Utils()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val map = HashMap<String, Any>()
    private var coins = 0
    private var rewardedAd: RewardedAd? = null
    private var isRewarded = false
    private var userName: String? = null
    private var userEmail: String? = null
    private var payCoins: String? = null
    private var payEmail: String? = null
    private var isWithdrawRequest = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithdrawBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        mAdView = AdView(this)
        mAdView.adUnitId = getString(R.string.bannerAds)
        mAdView.setAdSize(getAdSize())
        binding.adView.addView(mAdView)
        mAdView.loadAd(adRequest)
        loadInterstitialAds()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this@WithdrawActivity)
                } else {
                    this@WithdrawActivity.finish()
                    overridePendingTransition(R.anim.from_left, R.anim.to_right)
                }
            }
        })

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        userName = intent.getStringExtra("name")
        userEmail = intent.getStringExtra("email")
        coins = intent.getIntExtra("coins", 0)
        binding.coins.text = coins.toString()

        binding.itemEmail.hint = "Enter paypal emil"
        binding.paypal.setOnClickListener {
            type = "Paypal"
            binding.itemEmail.hint = "Enter paypal email"
            binding.paypal.strokeColor = Color.parseColor("#009688")
            binding.payoneer.strokeColor = Color.parseColor("#E4E4E4")
        }
        binding.payoneer.setOnClickListener {
            type = "Payoneer"
            binding.itemEmail.hint = "Enter payoneer email"
            binding.payoneer.strokeColor = Color.parseColor("#009688")
            binding.paypal.strokeColor = Color.parseColor("#E4E4E4")
        }

        binding.sendRequest.setOnClickListener {
            if (coins <= 5000)
                utils.showToast(this@WithdrawActivity, "Earn 5,000 coins")
            else if (!utils.isInternetConnected(this@WithdrawActivity))
                utils.showToast(this@WithdrawActivity, "No Internet Connection")
            else {
                payCoins = binding.coinsField.text.toString()
                payEmail = binding.emailField.text.toString()
                val emailPattern = "[a-zA-Z0-9._-]+@gmail.com"
                if (payCoins!!.isBlank())
                    binding.coinsField.error = "Enter coins value"
                else if (payCoins!!.toInt() < 5000 || payCoins!!.toInt() > coins)
                    binding.coinsField.error = "Enter valid coins value"
                else if (payEmail!!.isBlank())
                    binding.emailField.error = "Enter email address"
                else if (!payEmail!!.matches(emailPattern.toRegex()))
                    binding.emailField.error = "Enter a valid gmail address"
                else {
                    loadRewardedAds()
                    isWithdrawRequest = true
                }
            }
        }
        binding.pendingPayment.setOnClickListener {
            loadRewardedAds()
            isWithdrawRequest = false
        }

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
                                this@WithdrawActivity.finish()
                                overridePendingTransition(R.anim.from_left, R.anim.to_right)
                            }
                        }
                }
            })
    }

    private fun requestWithdrawal(payCoins: String?, payEmail: String?) {
        utils.showToast(this@WithdrawActivity, "Requesting withdrawal....")
        firebaseAuth.currentUser?.let { it1 ->
            firestore.collection("withdraw_request").document(it1.uid)
        }?.get()?.addOnCompleteListener { it2 ->
            if (it2.isSuccessful) {
                map["name"] = userName.toString()
                map["userEmail"] = userEmail.toString()
                map["payCoins"] = payCoins!!.toInt()
                map["payEmail"] = payEmail!!
                map["paymentGateway"] = type
                map["requestDate"] = FieldValue.serverTimestamp()
                if (it2.result.exists()) {
                    //data already exist in the database
                    firestore.collection("withdraw_request")
                        .document(firebaseAuth.currentUser!!.uid)
                        .update(
                            "payCoins",
                            FieldValue.increment(payCoins.toDouble()),
                            "paymentGateway", type
                        ).addOnCompleteListener { it3 ->
                            if (it3.isSuccessful) {
                                coins -= payCoins.toInt()
                                firestore.collection("users")
                                    .document(firebaseAuth.currentUser!!.uid)
                                    .update("coins", coins)
                                    .addOnCompleteListener { it4 ->
                                        if (it4.isSuccessful) {
                                            binding.coins.text = coins.toString()
                                            utils.showToast(
                                                this@WithdrawActivity,
                                                "Request Successful"
                                            )
                                        } else {
                                            utils.showToast(
                                                this@WithdrawActivity,
                                                "Something went wrong"
                                            )
                                        }
                                    }
                            }
                        }
                } else {
                    //data doesn't exist in the database
                    firestore.collection("withdraw_request")
                        .document(firebaseAuth.currentUser!!.uid)
                        .set(map).addOnCompleteListener { it5 ->
                            if (it5.isSuccessful) {
                                coins -= payCoins.toInt()
                                firestore.collection("users")
                                    .document(firebaseAuth.currentUser!!.uid)
                                    .update("coins", coins)
                                    .addOnCompleteListener { it6 ->
                                        if (it6.isSuccessful) {
                                            binding.coins.text = coins.toString()
                                            utils.showToast(
                                                this@WithdrawActivity,
                                                "Request Successful"
                                            )
                                        } else {
                                            utils.showToast(
                                                this@WithdrawActivity,
                                                "Something went wrong"
                                            )
                                        }
                                    }
                            }
                        }
                }
            } else {
                utils.showToast(this@WithdrawActivity, "Something went wrong")
            }
        }
    }

    private fun pendingRequest() {
        utils.showToast(this@WithdrawActivity, "Pending payment checking....")
        firebaseAuth.currentUser?.let { it1 ->
            firestore.collection("withdraw_request").document(it1.uid)
        }?.get()?.addOnCompleteListener {
            if (it.isSuccessful) {
                if (it.result.exists()) {
                    val builder = AlertDialog.Builder(this@WithdrawActivity)
                    builder.setCancelable(true)
                    builder.setTitle("Pending Payment Details: ")
                    builder.setMessage(
                        "${
                            it.result.get("name")
                        } \n${
                            it.result.get("payEmail")
                        }\nCoins: ${
                            it.result.get("payCoins")
                        }\nGateway: ${
                            it.result.get("paymentGateway")
                        }"
                    )
                    val alertDialog = builder.create()
                    alertDialog.window?.setWindowAnimations(R.style.AnimationPopup)
                    alertDialog.show()
                } else {
                    utils.showToast(
                        this@WithdrawActivity,
                        "No pending payment found"
                    )
                }
            } else {
                utils.showToast(
                    this@WithdrawActivity,
                    "Something went wrong"
                )
            }
        }
    }

    private fun loadRewardedAds() {
        utils.showToast(this@WithdrawActivity, "Ads loading...")
        RewardedAd.load(this, getString(R.string.rewardedAds), adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    utils.showToast(this@WithdrawActivity, "No ads found")
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            if (isRewarded) {
                                if (isWithdrawRequest) requestWithdrawal(payCoins, payEmail)
                                if (!isWithdrawRequest) pendingRequest()
                            }
                        }
                    }
                    rewardedAd!!.show(this@WithdrawActivity) { isRewarded = true }
                }
            })
    }
}
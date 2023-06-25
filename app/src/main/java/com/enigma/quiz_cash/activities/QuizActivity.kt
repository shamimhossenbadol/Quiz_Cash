package com.enigma.quiz_cash.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.enigma.quiz_cash.DatabaseHelper
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.Utils
import com.enigma.quiz_cash.databinding.ActivityQuizBinding
import com.enigma.quiz_cash.databinding.AdsDialogBinding
import com.enigma.quiz_cash.models.QuestionModel
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

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private var questions: List<QuestionModel> = ArrayList()
    private var count: Int? = 0
    private var userReward: Int? = 0
    private var mediaPlayer: MediaPlayer? = null
    private val numbers = mutableListOf<Int>()
    private var earnedCoins: Int? = 0
    private var life: Int? = 4
    private var answeredQuestions: Int? = 0
    private var correctAnswer: Int? = 0
    private var wrongAnswer: Int? = 0
    private lateinit var adRequest: AdRequest
    private var rewardedAd: RewardedAd? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var isRewarded: Boolean = false
    private var homeClicked: Boolean = false
    private lateinit var mAdView: AdView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
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
                    mInterstitialAd?.show(this@QuizActivity)
                    homeClicked = true
                } else {
                    this@QuizActivity.finish()
                    overridePendingTransition(R.anim.from_left, R.anim.to_right)
                }
            }
        })

        earnedCoins = intent.getIntExtra("coins", 0)
        binding.coins.text = earnedCoins.toString()
        val topic = intent.getStringExtra("title")!!
        val databaseHelper = DatabaseHelper.getInstance(this)
        databaseHelper.getDatabaseDao().getQuestions(topic).observe(this) { updatedQuestions ->
            questions = updatedQuestions
            while (numbers.size < 20) {
                val randomNumber = (questions.indices).random()
                if (randomNumber !in numbers) numbers.add(randomNumber)
            }
            setQuestions()
            binding.progressBar.max = (numbers.size - 1)
            binding.progressBar.progress = count!!
        }

        val colorCode = intent.getStringExtra("color")
        val colorStateList = ColorStateList.valueOf(Color.parseColor(colorCode))
        binding.textBar.setBackgroundColor(Color.parseColor(colorCode))
        binding.next.setCardBackgroundColor(colorStateList)
        binding.progressBar.progressTintList = colorStateList

        binding.next.setOnClickListener {
            val utils = Utils()
            if (utils.isInternetConnected(this)) {
                if (count!! >= 0 && count!! < (numbers.size - 1)) {
                    count = count!! + 1
                    binding.progressBar.progress = count as Int
                    clearPreviousStatus()
                    setQuestions()
                    if (count!! % 3 == 0 && life!! > 0) {
                        if (mInterstitialAd != null) mInterstitialAd?.show(this)
                        else loadInterstitialAds()
                    }
                } else {
                    val intent = Intent(this, ResultsActivity::class.java)
                    intent.putExtra("answeredQuestions", answeredQuestions)
                    intent.putExtra("correctAnswer", correctAnswer)
                    intent.putExtra("wrongAnswer", wrongAnswer)
                    intent.putExtra("rewardedCoins", userReward)
                    startActivity(intent)
                    overridePendingTransition(R.anim.from_right, R.anim.to_left)
                    this@QuizActivity.finish()
                }
            } else utils.showToast(this@QuizActivity, "No Internet Connection")
        }
        binding.radioGroup.setOnCheckedChangeListener { _, _ ->
            binding.option1.isClickable = false
            binding.option2.isClickable = false
            binding.option3.isClickable = false
            binding.option4.isClickable = false
            var userAnswer: String? = null
            if (binding.option1.isChecked) {
                userAnswer = "A"
                answeredQuestions = answeredQuestions?.plus(1)
            }
            if (binding.option2.isChecked) {
                userAnswer = "B"
                answeredQuestions = answeredQuestions?.plus(1)
            }
            if (binding.option3.isChecked) {
                userAnswer = "C"
                answeredQuestions = answeredQuestions?.plus(1)
            }
            if (binding.option4.isChecked) {
                userAnswer = "D"
                answeredQuestions = answeredQuestions?.plus(1)
            }
            checkUserAnswer(userAnswer, questions[count!!].answer)
        }
    }

    private fun getAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun clearPreviousStatus() {
        binding.radioGroup.clearCheck()
        binding.option1.isClickable = true
        binding.option2.isClickable = true
        binding.option3.isClickable = true
        binding.option4.isClickable = true

        binding.option1.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        binding.option2.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        binding.option3.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        binding.option4.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        binding.option1.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option2.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option3.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option4.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun setQuestions() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.questions_anim)
        binding.question.startAnimation(animation)
        binding.option1.startAnimation(animation)
        binding.option2.startAnimation(animation)
        binding.option3.startAnimation(animation)
        binding.option4.startAnimation(animation)
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.next)
        mediaPlayer?.start()
        binding.question.text = questions[numbers[count!!]].question?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        binding.option1.text = questions[numbers[count!!]].option_1?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        binding.option2.text = questions[numbers[count!!]].option_2?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        binding.option3.text = questions[numbers[count!!]].option_3?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        binding.option4.text = questions[numbers[count!!]].option_4?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun checkUserAnswer(userAnswer: String?, systemAnswer: String?) {
        if (userAnswer.equals("A")) {
            if (userAnswer.equals(systemAnswer)) {
                binding.option1.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                updateCoins()
                correctAnswer = correctAnswer?.plus(1)
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.correct)
                mediaPlayer?.start()
            } else {
                binding.option1.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.wrong)
                mediaPlayer?.start()
                wrongAnswer = wrongAnswer?.plus(1)
                if (life!! > 0) life = life?.minus(1)
                else showAdsDialog()
            }
            binding.option1.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        if (userAnswer.equals("B")) {
            if (userAnswer.equals(systemAnswer)) {
                binding.option2.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                updateCoins()
                correctAnswer = correctAnswer?.plus(1)
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.correct)
                mediaPlayer?.start()
            } else {
                binding.option2.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.wrong)
                mediaPlayer?.start()
                wrongAnswer = wrongAnswer?.plus(1)
                if (life!! > 0) life = life?.minus(1)
                else showAdsDialog()
            }
            binding.option2.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        if (userAnswer.equals("C")) {
            if (userAnswer.equals(systemAnswer)) {
                binding.option3.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                updateCoins()
                correctAnswer = correctAnswer?.plus(1)
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.correct)
                mediaPlayer?.start()
            } else {
                binding.option3.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.wrong)
                mediaPlayer?.start()
                wrongAnswer = wrongAnswer?.plus(1)
                if (life!! > 0) life = life?.minus(1)
                else showAdsDialog()
            }
            binding.option3.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        if (userAnswer.equals("D")) {
            if (userAnswer.equals(systemAnswer)) {
                binding.option4.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                updateCoins()
                correctAnswer = correctAnswer?.plus(1)
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.correct)
                mediaPlayer?.start()
            } else {
                binding.option4.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.wrong)
                mediaPlayer?.start()
                wrongAnswer = wrongAnswer?.plus(1)
                if (life!! > 0) life = life?.minus(1)
                else showAdsDialog()
            }
            binding.option4.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        if (systemAnswer.equals("A")) {
            binding.option1.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            binding.option1.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        if (systemAnswer.equals("B")) {
            binding.option2.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            binding.option2.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        if (systemAnswer.equals("C")) {
            binding.option3.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            binding.option3.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        if (systemAnswer.equals("D")) {
            binding.option4.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            binding.option4.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun showAdsDialog() {
        val builder = AlertDialog.Builder(this)
        val adsBinding = AdsDialogBinding.inflate(layoutInflater)
        builder.setView(adsBinding.root)
        builder.setCancelable(false)
        val alertDialog = builder.create()
        adsBinding.ads.setOnClickListener {
            loadRewardedAds(alertDialog)
        }
        adsBinding.home.setOnClickListener {
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(this)
                homeClicked = true
            } else {
                this@QuizActivity.finish()
                overridePendingTransition(R.anim.from_left, R.anim.to_right)
            }
        }
        alertDialog.window?.setWindowAnimations(R.style.AnimationPopup)
        alertDialog.show()
    }

    private fun updateCoins() {
        userReward = userReward?.plus(5)
        earnedCoins = earnedCoins?.plus(5)
        firebaseAuth.currentUser?.let { firebaseFirestore.collection("users").document(it.uid) }
            ?.update("coins", FieldValue.increment(5))
        binding.coins.text = earnedCoins.toString()
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
                                if (homeClicked) {
                                    this@QuizActivity.finish()
                                    overridePendingTransition(R.anim.from_left, R.anim.to_right)
                                } else loadInterstitialAds()
                            }
                        }
                }
            })
    }

    private fun loadRewardedAds(alertDialog: AlertDialog) {
        utils.showToast(this@QuizActivity, "Ads loading...")
        RewardedAd.load(this, getString(R.string.rewardedAds), adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    utils.showToast(this@QuizActivity, "No ads found")
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            if (isRewarded) {
                                alertDialog.dismiss()
                                life = 4
                            }
                        }
                    }
                    rewardedAd!!.show(this@QuizActivity) { isRewarded = true }
                }
            })
    }
}

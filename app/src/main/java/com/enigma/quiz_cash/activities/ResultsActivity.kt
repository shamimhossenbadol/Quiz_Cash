package com.enigma.quiz_cash.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.enigma.quiz_cash.R
import com.enigma.quiz_cash.databinding.ActivityResultsBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding
    private var answeredQuestions: Int? = 0
    private var correctAnswer: Int? = 0
    private var wrongAnswer: Int? = 0
    private var userReward: Int? = 0
    private lateinit var adRequest: AdRequest
    private lateinit var mAdView: AdView
    private var mInterstitialAd: InterstitialAd? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        answeredQuestions = intent.getIntExtra("answeredQuestions", 0)
        correctAnswer = intent.getIntExtra("correctAnswer", 0)
        wrongAnswer = intent.getIntExtra("wrongAnswer", 0)
        userReward = intent.getIntExtra("rewardedCoins", 0)

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        mAdView = AdView(this)
        mAdView.adUnitId = getString(R.string.bannerAds)
        mAdView.setAdSize(getAdSize())
        binding.adview.addView(mAdView)
        mAdView.loadAd(adRequest)
        InterstitialAd.load(this, getString(R.string.interstitialAds), adRequest,
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
                                this@ResultsActivity.finish()
                            }
                        }
                }
            })

        binding.coins.text = userReward.toString()
        val entries = listOf(
            PieEntry(correctAnswer!!.toFloat(), "Correct"),
            PieEntry(wrongAnswer!!.toFloat(), "Wrong"),
            PieEntry((20 - answeredQuestions!!).toFloat(), "Not Answered")
        )
        val dataSet = PieDataSet(entries, null)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 25.0f
        dataSet.colors = listOf(Color.parseColor("#47ba40"), Color.RED, Color.GRAY)

        val legend = binding.pieChart.legend
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        //legend.setDrawInside(false)
        dataSet.setDrawValues(false)
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.isDrawHoleEnabled = true
        binding.pieChart.setHoleColor(Color.WHITE)
        binding.pieChart.setCenterTextSize(25.0f)
        binding.pieChart.setEntryLabelTextSize(0f)
        binding.pieChart.setCenterTextTypeface(ResourcesCompat.getFont(this, R.font.cambria_bold))
        binding.pieChart.setEntryLabelTypeface(
            ResourcesCompat.getFont(
                this,
                R.font.cambria_regular
            )
        )
        binding.pieChart.description.isEnabled = false
        val percentageCorrect = (correctAnswer!!.toFloat() / answeredQuestions!!) * 100
        binding.pieChart.centerText = String.format("Accuracy: \n%.1f%%", percentageCorrect)
        binding.pieChart.invalidate()
        binding.home.setOnClickListener {
            if (mInterstitialAd != null) mInterstitialAd?.show(this)
            else this@ResultsActivity.finish()
        }
    }

    private fun getAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }
}
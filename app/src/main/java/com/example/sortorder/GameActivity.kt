package com.example.sortorder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.hypot

class GameActivity : AppCompatActivity() {

    private lateinit var gameRoot: FrameLayout
    private lateinit var tvLevel: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvBestScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvCombo: TextView
    private lateinit var tvRule: TextView
    private lateinit var btnHint: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var boxContainer: FlowLayout
    private lateinit var gameOverOverlay: FrameLayout
    private lateinit var winOverlay: FrameLayout
    private lateinit var adEntitlement: AdEntitlement
    private lateinit var bannerAdHelper: BannerAdHelper
    private lateinit var rewardedInterstitialAdHelper: RewardedInterstitialAdHelper

    private var currentLevel = 1
    private val maxLevel = 9
    private var score = 0
    private var bestScore = 0
    private var timeLeft = 35
    private var timerDuration = 35
    private var selectedBoxIndex: Int? = null
    private var isLevelFinished = false
    private var checksUsed = 0
    private var hintsRemaining = 1
    private var hintsUsed = 0
    private var combo = 0
    private var soundEnabled = true
    private var isSwapping = false
    private var soundPool: SoundPool? = null
    private var correctSoundId = 0
    private var wrongSoundId = 0

    private var targetNumbers: List<Int> = emptyList()
    private var currentNumbers: MutableList<Int> = mutableListOf()
    private var timer: CountDownTimer? = null
    private val boxViews = mutableListOf<FrameLayout>()
    private val revealedHintIndices = mutableSetOf<Int>()
    private lateinit var coinWallet: CoinWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        FullScreenHelper.apply(this)

        initViews()
        coinWallet = CoinWallet(this)
        adEntitlement = AdEntitlement(this)
        bannerAdHelper = BannerAdHelper(
            this,
            findViewById(R.id.gameAdBanner),
            AdMobIds.GAME_BANNER,
            adEntitlement
        )
        rewardedInterstitialAdHelper = RewardedInterstitialAdHelper(
            this,
            AdMobIds.REWARDED_INTERSTITIAL
        )
        bannerAdHelper.load()
        if (!adEntitlement.isAdFree()) {
            rewardedInterstitialAdHelper.load()
        }
        loadBestScore()
        initSounds()
        setupListeners()
        startLevel()
    }

    private fun initViews() {
        gameRoot = findViewById(R.id.gameRoot)
        tvLevel = findViewById(R.id.tvLevel)
        tvScore = findViewById(R.id.tvScore)
        tvBestScore = findViewById(R.id.tvBestScore)
        tvTimer = findViewById(R.id.tvTimer)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvCombo = findViewById(R.id.tvCombo)
        tvRule = findViewById(R.id.tvRule)
        btnHint = findViewById(R.id.btnHint)
        progressBar = findViewById(R.id.progressBar)
        boxContainer = findViewById(R.id.boxContainer)
        boxContainer.clipChildren = false
        boxContainer.clipToPadding = false
        gameOverOverlay = findViewById(R.id.gameOverOverlay)
        winOverlay = findViewById(R.id.winOverlay)
    }

    private fun loadBestScore() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        bestScore = prefs.getInt(KEY_BEST_SCORE, 0)
    }

    private fun saveBestScore() {
        if (score <= bestScore) return
        bestScore = score
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BEST_SCORE, bestScore)
            .apply()
    }

    private fun initSounds() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()
        correctSoundId = soundPool?.load(this, R.raw.right_answer, 1) ?: 0
        wrongSoundId = soundPool?.load(this, R.raw.wrong_anwser, 1) ?: 0
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnCheck).setOnClickListener { checkOrder() }
        btnHint.setOnClickListener { useHint() }

        findViewById<View>(R.id.btnWatchAd).setOnClickListener {
            showRewardedAdForExtraTime()
        }

        findViewById<View>(R.id.btnUseCoins).setOnClickListener {
            if (coinWallet.spend(CoinWallet.EXTRA_TIME_COST)) {
                continueWithExtraTime(COIN_EXTRA_TIME_SECONDS)
            } else {
                showNotEnoughCoinsDialog()
            }
        }

        findViewById<View>(R.id.btnGoHome).setOnClickListener { finish() }

        findViewById<View>(R.id.btnNextLevel).setOnClickListener {
            winOverlay.visibility = View.GONE
            currentLevel++
            if (currentLevel > maxLevel) {
                Toast.makeText(this, R.string.all_levels_completed, Toast.LENGTH_LONG).show()
                finish()
            } else {
                startLevel()
            }
        }
    }

    private fun getNumDigits(): Int = (currentLevel + 2).coerceAtMost(7)

    private fun getTimeForLevel(): Int {
        val baseTime = 40 + (getNumDigits() - 3) * 10
        return baseTime + if (usesAdjacentSwapRule()) 15 else 0
    }

    private fun usesAdjacentSwapRule(): Boolean = currentLevel >= 7

    private fun startLevel() {
        timerDuration = getTimeForLevel()
        timeLeft = timerDuration
        selectedBoxIndex = null
        isLevelFinished = false
        checksUsed = 0
        hintsUsed = 0
        hintsRemaining = combo
        revealedHintIndices.clear()

        targetNumbers = (0..9).shuffled().take(getNumDigits())
        currentNumbers = targetNumbers.shuffled().toMutableList()
        while (currentNumbers == targetNumbers) {
            currentNumbers.shuffle()
        }

        tvLevel.text = getString(R.string.level_format, currentLevel)
        tvScore.text = score.toString()
        tvBestScore.text = bestScore.toString()
        tvFeedback.text = ""
        updateCombo()
        updateHintButton()
        tvRule.text = if (usesAdjacentSwapRule()) {
            getString(R.string.rule_adjacent)
        } else {
            ""
        }
        progressBar.max = timerDuration
        progressBar.progress = timeLeft
        createBoxes()
        updateTimerDisplay()
        startTimer()
    }

    private fun createBoxes() {
        boxContainer.removeAllViews()
        boxViews.clear()

        val numDigits = currentNumbers.size
        val boxSizeDp = when {
            numDigits <= 4 -> 64
            numDigits <= 6 -> 54
            else -> 46
        }
        val marginDp = if (numDigits <= 5) 6 else 4
        val textSizeSp = if (numDigits <= 5) 28f else 24f
        val density = resources.displayMetrics.density
        val boxSizePx = (boxSizeDp * density).toInt()
        val marginPx = (marginDp * density).toInt()

        currentNumbers.indices.forEach { index ->
            val box = FrameLayout(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(boxSizePx, boxSizePx).apply {
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                setBackgroundResource(R.drawable.bg_slot_filled)
                isClickable = true
                isFocusable = true

                addView(TextView(this@GameActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = currentNumbers[index].toString()
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
                    typeface = Typeface.DEFAULT_BOLD
                    tag = NUMBER_TEXT_TAG
                })

                setOnClickListener {
                    if (!isLevelFinished && gameOverOverlay.visibility != View.VISIBLE) {
                        selectOrSwap(index)
                    }
                }
            }
            boxViews.add(box)
            boxContainer.addView(box)
        }
    }

    private fun selectOrSwap(index: Int) {
        if (isSwapping) return
        val firstIndex = selectedBoxIndex
        if (firstIndex == null) {
            selectedBoxIndex = index
            boxViews[index].setBackgroundResource(R.drawable.bg_mystery_box_selected)
            return
        }

        if (firstIndex == index) {
            clearSelection()
            return
        }

        if (usesAdjacentSwapRule() && kotlin.math.abs(firstIndex - index) != 1) {
            clearSelection()
            playWrongSound()
            tvFeedback.setTextColor(Color.parseColor("#FFB45C"))
            tvFeedback.setText(R.string.adjacent_swap_only)
            shakeView(boxContainer)
            return
        }

        clearSelection()
        tvFeedback.text = ""
        performAnimatedSwap(firstIndex, index)
    }

    private fun performAnimatedSwap(idx1: Int, idx2: Int) {
        animateNumberSwap(idx1, idx2) {
            val temp = currentNumbers[idx1]
            currentNumbers[idx1] = currentNumbers[idx2]
            currentNumbers[idx2] = temp
        }
    }

    private fun animateNumberSwap(idx1: Int, idx2: Int, swapData: () -> Unit) {
        isSwapping = true
        val box1 = boxViews[idx1]
        val box2 = boxViews[idx2]
        val dx = (box2.left - box1.left).toFloat()
        val dy = (box2.top - box1.top).toFloat()
        val distance = hypot(dx, dy)
        val arcHeight = (distance * 0.28f).coerceIn(dp(18f), dp(46f))
        val direction = if (dx >= 0f) 1f else -1f

        val travel = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(
                    box1,
                    View.TRANSLATION_X,
                    View.TRANSLATION_Y,
                    createSwapPath(dx, dy, arcHeight)
                ),
                ObjectAnimator.ofFloat(
                    box2,
                    View.TRANSLATION_X,
                    View.TRANSLATION_Y,
                    createSwapPath(-dx, -dy, arcHeight)
                ),
                ObjectAnimator.ofFloat(box1, View.SCALE_X, 1f, 1.14f, 1.06f),
                ObjectAnimator.ofFloat(box1, View.SCALE_Y, 1f, 1.14f, 1.06f),
                ObjectAnimator.ofFloat(box2, View.SCALE_X, 1f, 1.14f, 1.06f),
                ObjectAnimator.ofFloat(box2, View.SCALE_Y, 1f, 1.14f, 1.06f),
                ObjectAnimator.ofFloat(box1, View.ROTATION, 0f, 8f * direction, 0f),
                ObjectAnimator.ofFloat(box2, View.ROTATION, 0f, -8f * direction, 0f),
                ObjectAnimator.ofFloat(box1, View.TRANSLATION_Z, 0f, dp(12f), 0f),
                ObjectAnimator.ofFloat(box2, View.TRANSLATION_Z, 0f, dp(12f), 0f)
            )
            duration = SWAP_TRAVEL_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
        }

        travel.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                swapData()
                updateBoxNumber(idx1)
                updateBoxNumber(idx2)
                resetBoxBackground(idx1)
                resetBoxBackground(idx2)
                resetSwapTransform(box1)
                resetSwapTransform(box2)

                boxContainer.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                spawnSwapParticles(box1)
                spawnSwapParticles(box2)
                playSwapLanding(box1, box2)
            }
        })
        travel.start()
    }

    private fun createSwapPath(dx: Float, dy: Float, arcHeight: Float): Path {
        val distance = hypot(dx, dy).coerceAtLeast(1f)
        val normalX = -dy / distance
        val normalY = dx / distance
        return Path().apply {
            moveTo(0f, 0f)
            quadTo(
                dx * 0.5f + normalX * arcHeight,
                dy * 0.5f + normalY * arcHeight,
                dx,
                dy
            )
        }
    }

    private fun resetSwapTransform(view: View) {
        view.translationX = 0f
        view.translationY = 0f
        view.translationZ = 0f
        view.rotation = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    private fun playSwapLanding(box1: View, box2: View) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(box1, View.SCALE_X, 1f, 0.9f, 1.08f, 1f),
                ObjectAnimator.ofFloat(box1, View.SCALE_Y, 1f, 0.86f, 1.1f, 1f),
                ObjectAnimator.ofFloat(box2, View.SCALE_X, 1f, 0.9f, 1.08f, 1f),
                ObjectAnimator.ofFloat(box2, View.SCALE_Y, 1f, 0.86f, 1.1f, 1f)
            )
            duration = SWAP_LANDING_DURATION_MS
            interpolator = OvershootInterpolator(1.35f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    resetSwapTransform(box1)
                    resetSwapTransform(box2)
                    isSwapping = false
                }
            })
            start()
        }
    }

    private fun spawnSwapParticles(anchor: View) {
        val rootLocation = IntArray(2)
        val anchorLocation = IntArray(2)
        gameRoot.getLocationInWindow(rootLocation)
        anchor.getLocationInWindow(anchorLocation)
        val cx = anchorLocation[0] - rootLocation[0] + anchor.width / 2f
        val cy = anchorLocation[1] - rootLocation[1] + anchor.height / 2f
        val size = dp(5f).toInt()
        val colors = intArrayOf(
            Color.parseColor("#22D3EE"),
            Color.parseColor("#FDE047"),
            Color.parseColor("#34D399"),
            Color.parseColor("#A78BFA"),
            Color.parseColor("#FB923C"),
            Color.parseColor("#F472B6")
        )
        val particleCount = 6
        for (i in 0 until particleCount) {
            val dot = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colors[i % colors.size])
                }
                x = cx - size / 2f
                y = cy - size / 2f
            }
            gameRoot.addView(dot)
            val angle = Math.toRadians((i * 360.0 / particleCount) + 30.0)
            val dist = anchor.width * 0.9f
            val tx = (Math.cos(angle) * dist).toFloat()
            val ty = (Math.sin(angle) * dist).toFloat()
            dot.animate()
                .translationXBy(tx)
                .translationYBy(ty)
                .alpha(0f)
                .scaleX(0.2f)
                .scaleY(0.2f)
                .setDuration(350)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { gameRoot.removeView(dot) }
                .start()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun updateBoxNumber(index: Int) {
        boxViews[index].findViewWithTag<TextView>(NUMBER_TEXT_TAG).text =
            currentNumbers[index].toString()
    }

    private fun clearSelection() {
        selectedBoxIndex?.let { resetBoxBackground(it) }
        selectedBoxIndex = null
    }

    private fun resetBoxBackground(index: Int) {
        if (currentNumbers[index] != targetNumbers[index]) {
            revealedHintIndices.remove(index)
        }
        boxViews[index].setBackgroundResource(
            if (index in revealedHintIndices) {
                R.drawable.bg_slot_correct
            } else {
                R.drawable.bg_slot_filled
            }
        )
    }

    private fun checkOrder() {
        if (isLevelFinished || isSwapping || gameOverOverlay.visibility == View.VISIBLE) return

        clearSelection()
        val correctCount = currentNumbers.indices.count {
            currentNumbers[it] == targetNumbers[it]
        }
        checksUsed++

        if (correctCount == targetNumbers.size) {
            playCorrectSound()
            tvFeedback.setTextColor(Color.parseColor("#6EF2A3"))
            tvFeedback.setText(R.string.correct_order)
            onWin()
        } else {
            playWrongSound()
            tvFeedback.setTextColor(Color.parseColor("#FFB45C"))
            tvFeedback.text = getString(R.string.mastermind_feedback, correctCount)
            shakeView(boxContainer)
        }
    }

    private fun useHint() {
        if (isLevelFinished || isSwapping || hintsRemaining <= 0 ||
            gameOverOverlay.visibility == View.VISIBLE
        ) {
            return
        }

        clearSelection()
        // Find wrong indices that have NOT been hinted before
        val wrongIndices = currentNumbers.indices.filter {
            currentNumbers[it] != targetNumbers[it] && it !in revealedHintIndices
        }
        // Fallback: if all non-hinted are correct, try any wrong index
        val candidates = wrongIndices.ifEmpty {
            currentNumbers.indices.filter { currentNumbers[it] != targetNumbers[it] }
        }
        if (candidates.isEmpty()) return

        val targetIndex = candidates.random()
        val sourceIndex = currentNumbers.indexOf(targetNumbers[targetIndex])
        if (sourceIndex < 0) return

        animateNumberSwap(targetIndex, sourceIndex) {
            val displacedValue = currentNumbers[targetIndex]
            currentNumbers[targetIndex] = currentNumbers[sourceIndex]
            currentNumbers[sourceIndex] = displacedValue
            revealedHintIndices.add(targetIndex)
        }

        hintsRemaining--
        hintsUsed++
        combo = (combo - 1).coerceAtLeast(0)
        updateCombo()
        updateHintButton()
        tvFeedback.setTextColor(Color.parseColor("#6EF2A3"))
        tvFeedback.text = getString(R.string.hint_revealed, targetIndex + 1)
    }

    private fun updateHintButton() {
        btnHint.text = getString(R.string.hint_button, hintsRemaining)
        btnHint.alpha = if (hintsRemaining > 0) 1f else 0.45f
        btnHint.isEnabled = hintsRemaining > 0
    }

    private fun updateCombo() {
        tvCombo.text = getString(R.string.combo_format, combo)
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f,
            10f,
            -10f,
            8f,
            -8f,
            4f,
            -4f,
            0f
        ).apply {
            duration = 500
            start()
        }
    }

    private fun onWin() {
        timer?.cancel()
        isLevelFinished = true
        val stars = calculateStars()
        combo++
        val levelScore = timeLeft * 10 + currentLevel * 50 + combo * 25 + stars * 100
        score += levelScore
        tvScore.text = score.toString()
        updateCombo()
        saveBestScore()
        tvBestScore.text = bestScore.toString()
        findViewById<TextView>(R.id.tvWinStars).text = "★".repeat(stars) +
            "☆".repeat(3 - stars)
        val winStats = getString(
            R.string.win_stats,
            levelScore,
            checksUsed,
            combo
        )
        findViewById<TextView>(R.id.tvWinStats).text = winStats

        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val previousHighest = prefs.getInt(KEY_HIGHEST_LEVEL, 0)
        if (currentLevel > previousHighest) {
            prefs.edit().putInt(KEY_HIGHEST_LEVEL, currentLevel).apply()
        }

        boxContainer.postDelayed({
            winOverlay.alpha = 0f
            winOverlay.visibility = View.VISIBLE
            winOverlay.animate().alpha(1f).setDuration(300).start()
        }, 500)
    }

    private fun calculateStars(): Int {
        val ratio = timeLeft.toFloat() / timerDuration
        return when {
            ratio >= 0.5f && checksUsed <= 1 && hintsUsed == 0 -> 3
            ratio >= 0.25f && checksUsed <= 3 -> 2
            else -> 1
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(timeLeft * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1000L).toInt()
                updateTimerDisplay()
            }

            override fun onFinish() {
                timeLeft = 0
                updateTimerDisplay()
                showGameOver()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        tvTimer.text = getString(R.string.timer_format, timeLeft)
        progressBar.max = timerDuration
        progressBar.progress = timeLeft

        val remainingRatio = if (timerDuration > 0) {
            timeLeft.toFloat() / timerDuration
        } else {
            0f
        }

        val (progressDrawable, timerColor) = when {
            remainingRatio <= 0.2f ->
                R.drawable.bg_progress_game_red to Color.parseColor("#FF4B72")
            remainingRatio <= 0.5f ->
                R.drawable.bg_progress_game_yellow to Color.parseColor("#FFD54F")
            else ->
                R.drawable.bg_progress_game to Color.WHITE
        }
        progressBar.setProgressDrawableTiled(getDrawable(progressDrawable))
        tvTimer.setTextColor(timerColor)
    }

    private fun showGameOver() {
        timer?.cancel()
        combo = 0
        hintsRemaining = 0
        updateCombo()
        updateHintButton()
        clearSelection()
        saveBestScore()
        findViewById<TextView>(R.id.tvGameOverMsg).text =
            getString(R.string.game_over_message, currentLevel)
        gameOverOverlay.alpha = 0f
        gameOverOverlay.visibility = View.VISIBLE
        updateGameOverActions()
        gameOverOverlay.animate().alpha(1f).setDuration(300).start()
    }

    private fun updateGameOverActions() {
        findViewById<View>(R.id.btnWatchAd).visibility =
            if (adEntitlement.isAdFree()) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.tvExtraTimeCoinAction).text = getString(
            R.string.use_coins_for_time,
            CoinWallet.EXTRA_TIME_COST,
            coinWallet.getBalance()
        )
    }

    private fun showRewardedAdForExtraTime() {
        val watchAdButton = findViewById<View>(R.id.btnWatchAd)
        watchAdButton.isEnabled = false
        watchAdButton.alpha = 0.65f

        rewardedInterstitialAdHelper.show(
            onRewardEarned = {
                coinWallet.add(REWARDED_AD_COIN_REWARD)
                Toast.makeText(
                    this,
                    getString(R.string.coins_added, REWARDED_AD_COIN_REWARD),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onAdUnavailable = {
                watchAdButton.isEnabled = true
                watchAdButton.alpha = 1f
                Toast.makeText(this, R.string.rewarded_ad_not_ready, Toast.LENGTH_SHORT).show()
            },
            onAdClosed = { rewardEarned ->
                watchAdButton.isEnabled = true
                watchAdButton.alpha = 1f
                if (rewardEarned && gameOverOverlay.visibility == View.VISIBLE) {
                    continueWithExtraTime(REWARDED_AD_SECONDS)
                } else if (gameOverOverlay.visibility == View.VISIBLE) {
                    updateGameOverActions()
                }
            }
        )
    }

    private fun showNotEnoughCoinsDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.not_enough_coins)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun playCorrectSound() {
        if (soundEnabled && correctSoundId != 0) {
            soundPool?.play(correctSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun playWrongSound() {
        if (soundEnabled && wrongSoundId != 0) {
            soundPool?.play(wrongSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onResume() {
        super.onResume()
        soundEnabled = getSharedPreferences(SOUND_PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
        if (::bannerAdHelper.isInitialized) {
            bannerAdHelper.resume()
            bannerAdHelper.refreshVisibility()
        }
        if (::rewardedInterstitialAdHelper.isInitialized && !adEntitlement.isAdFree()) {
            rewardedInterstitialAdHelper.load()
        }
        if (::coinWallet.isInitialized && gameOverOverlay.visibility == View.VISIBLE) {
            updateGameOverActions()
        }
    }

    override fun onPause() {
        if (::bannerAdHelper.isInitialized) bannerAdHelper.pause()
        super.onPause()
    }

    private fun continueWithExtraTime(extraSeconds: Int) {
        gameOverOverlay.visibility = View.GONE
        timerDuration = extraSeconds
        timeLeft = extraSeconds
        updateTimerDisplay()
        startTimer()
    }

    override fun onDestroy() {
        timer?.cancel()
        if (::bannerAdHelper.isInitialized) bannerAdHelper.destroy()
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }

    companion object {
        private const val PREF_NAME = "mystery_game_prefs"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_HIGHEST_LEVEL = "highest_level_cleared"
        private const val NUMBER_TEXT_TAG = "numberText"
        private const val SOUND_PREF_NAME = "game_prefs"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val SWAP_TRAVEL_DURATION_MS = 420L
        private const val SWAP_LANDING_DURATION_MS = 220L
        private const val REWARDED_AD_SECONDS = 15
        private const val REWARDED_AD_COIN_REWARD = 15
        private const val COIN_EXTRA_TIME_SECONDS = 20
    }
}

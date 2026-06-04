package com.example.sortorder

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private lateinit var tvLevel: TextView
    private lateinit var tvHearts: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var boxContainer: LinearLayout
    private lateinit var numberPadContainer: LinearLayout
    private lateinit var gameOverOverlay: FrameLayout
    private lateinit var winOverlay: FrameLayout

    private var currentLevel = 1
    private val maxLevel = 9
    private var lives = 3
    private var score = 0
    private var timeLeft = 25
    private var maxTime = 25

    private var secretNumbers: List<Int> = emptyList()
    private var userGuesses: MutableList<Int?> = mutableListOf()
    private var selectedBoxIndex = -1
    private var timer: CountDownTimer? = null

    private val boxViews = mutableListOf<FrameLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)

        initViews()
        setupListeners()
        startLevel()
    }

    private fun initViews() {
        tvLevel = findViewById(R.id.tvLevel)
        tvHearts = findViewById(R.id.tvHearts)
        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        tvInstruction = findViewById(R.id.tvInstruction)
        progressBar = findViewById(R.id.progressBar)
        boxContainer = findViewById(R.id.boxContainer)
        numberPadContainer = findViewById(R.id.numberPadContainer)
        gameOverOverlay = findViewById(R.id.gameOverOverlay)
        winOverlay = findViewById(R.id.winOverlay)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnWatchAd).setOnClickListener {
            // Simulate watching ad: add 15s
            gameOverOverlay.visibility = View.GONE
            timeLeft += 15
            maxTime = timeLeft
            startTimer()
        }

        findViewById<View>(R.id.btnBuyPremium).setOnClickListener {
            // Simulate premium: add 30s
            gameOverOverlay.visibility = View.GONE
            timeLeft += 30
            maxTime = timeLeft
            startTimer()
        }

        findViewById<View>(R.id.btnGoHome).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnNextLevel).setOnClickListener {
            winOverlay.visibility = View.GONE
            currentLevel++
            if (currentLevel > maxLevel) {
                Toast.makeText(this, "Bạn đã hoàn thành tất cả!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                startLevel()
            }
        }
    }

    private fun getNumDigits(): Int {
        // Levels 1-3: 3 digits, 4-6: 4 digits, 7-9: 5 digits
        return when {
            currentLevel <= 3 -> 3
            currentLevel <= 6 -> 4
            else -> 5
        }
    }

    private fun getTimeForLevel(): Int {
        return when {
            currentLevel <= 3 -> 25
            currentLevel <= 6 -> 30
            else -> 35
        }
    }

    private fun startLevel() {
        val numDigits = getNumDigits()
        timeLeft = getTimeForLevel()
        maxTime = timeLeft

        // Generate secret numbers
        val available = (0..9).toMutableList()
        available.shuffle()
        secretNumbers = available.subList(0, numDigits)
        userGuesses = MutableList(numDigits) { null }
        selectedBoxIndex = 0

        updateUI()
        createBoxes(numDigits)
        createNumberPad()
        startTimer()
    }

    private fun updateUI() {
        tvLevel.text = getString(R.string.level_format, currentLevel, maxLevel)
        tvScore.text = "🏆 $score"
        tvInstruction.text = getString(R.string.instruction, getNumDigits())
        updateHearts()
        updateTimerDisplay()
    }

    private fun updateHearts() {
        val active = "❤️"
        val inactive = "🖤"
        val hearts = StringBuilder()
        for (i in 0 until 3) {
            hearts.append(if (i < lives) active else inactive)
        }
        tvHearts.text = hearts
    }

    private fun updateTimerDisplay() {
        tvTimer.text = "⏱ ${timeLeft}s"
        val progress = (timeLeft.toFloat() / maxTime * 100).toInt()
        progressBar.progress = progress

        if (timeLeft <= 10) {
            tvTimer.setTextColor(Color.parseColor("#E74C3C"))
            progressBar.progressDrawable = getDrawable(R.drawable.bg_progress_bar_red)
        } else {
            tvTimer.setTextColor(Color.WHITE)
            progressBar.progressDrawable = getDrawable(R.drawable.bg_progress_bar)
        }
    }

    private fun createBoxes(numDigits: Int) {
        boxContainer.removeAllViews()
        boxViews.clear()

        val boxSizeDp = when {
            numDigits <= 3 -> 90
            numDigits <= 4 -> 75
            else -> 65
        }
        val marginDp = 8
        val density = resources.displayMetrics.density
        val boxSizePx = (boxSizeDp * density).toInt()
        val marginPx = (marginDp * density).toInt()

        for (i in 0 until numDigits) {
            val box = FrameLayout(this).apply {
                val lp = LinearLayout.LayoutParams(boxSizePx, boxSizePx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
                layoutParams = lp
                setBackgroundResource(if (i == 0) R.drawable.bg_mystery_box_selected else R.drawable.bg_mystery_box)

                val tvQuestion = TextView(this@GameActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = "?"
                    setTextColor(Color.parseColor("#78909C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                    typeface = Typeface.DEFAULT_BOLD
                    tag = "questionText"
                }
                addView(tvQuestion)

                setOnClickListener { selectBox(i) }
            }
            boxViews.add(box)
            boxContainer.addView(box)
        }
    }

    private fun selectBox(index: Int) {
        selectedBoxIndex = index
        // Update box visuals
        for (i in boxViews.indices) {
            val box = boxViews[i]
            val guess = userGuesses[i]
            if (i == index) {
                box.setBackgroundResource(R.drawable.bg_mystery_box_selected)
            } else if (guess != null) {
                box.setBackgroundResource(R.drawable.bg_mystery_box_selected)
            } else {
                box.setBackgroundResource(R.drawable.bg_mystery_box)
            }
        }

        // Scale animation
        val scaleAnim = ScaleAnimation(
            1f, 1.1f, 1f, 1.1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 150
            repeatMode = ScaleAnimation.REVERSE
            repeatCount = 1
        }
        boxViews[index].startAnimation(scaleAnim)
    }

    private fun createNumberPad() {
        numberPadContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val buttonSizePx = (48 * density).toInt()
        val marginPx = (6 * density).toInt()

        // Row 1: 0-4
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Row 2: 5-9
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = marginPx }
        }

        for (num in 0..9) {
            val btn = TextView(this).apply {
                val lp = LinearLayout.LayoutParams(buttonSizePx, buttonSizePx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
                layoutParams = lp
                setBackgroundResource(R.drawable.bg_number_pad)
                gravity = Gravity.CENTER
                text = num.toString()
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.DEFAULT_BOLD

                setOnClickListener { onNumberPressed(num) }
            }

            if (num < 5) row1.addView(btn) else row2.addView(btn)
        }

        numberPadContainer.addView(row1)
        numberPadContainer.addView(row2)
    }

    private fun onNumberPressed(num: Int) {
        if (selectedBoxIndex < 0 || selectedBoxIndex >= userGuesses.size) return

        // Place number in selected box
        userGuesses[selectedBoxIndex] = num

        // Update box display
        val box = boxViews[selectedBoxIndex]
        val tvQuestion = box.findViewWithTag<TextView>("questionText")
        tvQuestion.text = num.toString()
        tvQuestion.setTextColor(Color.WHITE)
        tvQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)

        // Animate
        val scaleAnim = ScaleAnimation(
            0.8f, 1f, 0.8f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }
        box.startAnimation(scaleAnim)

        // Auto-advance to next empty box
        val nextEmpty = userGuesses.indexOfFirst { it == null }
        if (nextEmpty >= 0) {
            selectBox(nextEmpty)
        } else {
            // All boxes filled - check answer
            checkAnswer()
        }
    }

    private fun checkAnswer() {
        timer?.cancel()
        var allCorrect = true

        for (i in secretNumbers.indices) {
            val box = boxViews[i]
            if (userGuesses[i] == secretNumbers[i]) {
                box.setBackgroundResource(R.drawable.bg_mystery_box_correct)
            } else {
                box.setBackgroundResource(R.drawable.bg_mystery_box_wrong)
                allCorrect = false
            }
        }

        if (allCorrect) {
            // WIN
            score += timeLeft * 10 + currentLevel * 50
            tvScore.text = "🏆 $score"

            // Delay then show win
            boxContainer.postDelayed({
                winOverlay.visibility = View.VISIBLE
                val fadeIn = ObjectAnimator.ofFloat(winOverlay, "alpha", 0f, 1f)
                fadeIn.duration = 300
                fadeIn.start()
            }, 800)
        } else {
            // Wrong answer - lose a life
            lives--
            updateHearts()

            if (lives <= 0) {
                boxContainer.postDelayed({
                    showGameOver()
                }, 1000)
            } else {
                // Reset for retry with same secret
                boxContainer.postDelayed({
                    resetBoxes()
                    startTimer()
                }, 1200)
            }
        }
    }

    private fun resetBoxes() {
        userGuesses = MutableList(secretNumbers.size) { null }
        selectedBoxIndex = 0
        for (i in boxViews.indices) {
            val box = boxViews[i]
            box.setBackgroundResource(if (i == 0) R.drawable.bg_mystery_box_selected else R.drawable.bg_mystery_box)
            val tvQuestion = box.findViewWithTag<TextView>("questionText")
            tvQuestion.text = "?"
            tvQuestion.setTextColor(Color.parseColor("#78909C"))
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(timeLeft * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1000).toInt()
                updateTimerDisplay()
            }

            override fun onFinish() {
                timeLeft = 0
                updateTimerDisplay()
                showGameOver()
            }
        }.start()
    }

    private fun showGameOver() {
        timer?.cancel()
        val tvMsg = findViewById<TextView>(R.id.tvGameOverMsg)
        tvMsg.text = getString(R.string.game_over_message, currentLevel)
        gameOverOverlay.visibility = View.VISIBLE
        val fadeIn = ObjectAnimator.ofFloat(gameOverOverlay, "alpha", 0f, 1f)
        fadeIn.duration = 300
        fadeIn.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}

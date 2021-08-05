package com.example.aniquiz

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import kotlinx.android.synthetic.main.activity_aniquestion.*
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.random.Random


// to be removed
class AniQuestionActivity : AppCompatActivity(), View.OnClickListener
{
    // Visuals
    private val backgrounds = Globals.getBackgrounds()
    private var circularProgressDrawable: CircularProgressDrawable? = null
    private var roundedCorners: RoundedCorners? = null

    // Static refs
    private var preferences: SharedPreferences? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var optButtons: Array<TextView>? = null
    private var optIVs: Array<ImageView>? = null
    private var mQuestionList: ArrayList<AniQuestion>? = null

    // Quiz
    private var mCurrentPosition: Int = 1
    private var mSelectedOption: Int = 0
    private var mCorrectOption: Int = 0
    private var isConfirmed: Boolean = false
    private var mScore: Int by Delegates.observable(0) {property, oldValue, newValue ->
        // Update shown score whenever it changes
        tv_cscore.text = getString(R.string.score_counter, newValue)
    }
    private var baseScore: Int = 10
    private var questionScore: Int = 10



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aniquestion)

        // Set up rounded corners
        roundedCorners = RoundedCorners((8 * (applicationContext.resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt())

        circularProgressDrawable = CircularProgressDrawable(applicationContext)
        circularProgressDrawable?.strokeWidth = 5f
        circularProgressDrawable?.centerRadius = 30f
        circularProgressDrawable?.start()

        // Get preferences
        preferences = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

        // Start with score = 0
        mScore = 0

        // Get questions from previous activity
        mQuestionList = intent.getParcelableArrayListExtra(Globals.questionList)
        progress_bar.max = mQuestionList!!.size

        // Set base score based on pool size
        baseScore = 5 + mQuestionList!!.size / 10 + Globals.aniNames.size / 10

        // Collect buttons into arrays, attach listeners
        optButtons = arrayOf(tv_option_one, tv_option_two, tv_option_three, tv_option_four)
        optButtons!!.map { btn -> btn.setOnClickListener(this) }
        optIVs = arrayOf(iv_option_one, iv_option_two, iv_option_three, iv_option_four)
        optIVs!!.map { iv -> iv.setOnClickListener(this) }
        btn_submit.setOnClickListener(this)

        // Show the first question
        setQuestion()

        GlobalScope.launch {

            // Try to preload themes
            for ((qNum, mQuestion) in mQuestionList!!.withIndex())
            {
                if(Globals.themeSrc != ThemeSrc.Local && mQuestion.type == QType.Theme && mQuestion.link == null && qNum + 1 > mCurrentPosition)
                {
                    delay(5000L)
                    if(qNum + 1 > mCurrentPosition)
                    {
                        mQuestion.link = pullLinkBG(mQuestion)
                    }
                }
            }
        }
    }

    private fun setQuestion()
    {
        // Set up the current question


        if(preferences != null && preferences!!.getBoolean("random_bgs", false) && backgrounds != null)
        {
            // Get a random background
            sv_main.background = ContextCompat.getDrawable(this, backgrounds.random())
        }

        // Disable and clear everything, necessary elements will be enabled later
        btn_submit.isEnabled = false
        btn_plus.isEnabled = false
        btn_stop.isEnabled = false
        ll_timer.visibility = View.GONE
        iv_image.visibility = View.GONE
        ll_txtopts.visibility = View.GONE
        tl_imgopts.visibility = View.GONE
        clearOptions()
        btn_submit.text = getString(R.string.btn_submit)

        // Nothing selected yet
        isConfirmed = false

        // Get current question from the list
        val question = mQuestionList!!.get(mCurrentPosition - 1)

        // Show progress on progress bar
        progress_bar.progress = mCurrentPosition
        tv_progress.text = "$mCurrentPosition" + "/" + progress_bar.max

        // One of the options will be correct
        mCorrectOption = Random.nextInt(1, 5)

        // Get IDs for incorrect options
        val options = Globals.aniNames.keys.toMutableList().filter { i -> i != question.aniID }.shuffled().drop((Globals.aniNames.keys.size - 5))

        // Fill the incorrect options
        for (i in 0..3)
        {
            if(i != mCorrectOption - 1)
            {
                optButtons!![i].text = "Globals.aniNames[options[i]]"
                Glide.with(applicationContext).load(Globals.aniCovers[options[i]]).transform(roundedCorners).placeholder(circularProgressDrawable).into(optIVs!![i])
            }
        }

        // Fill the correct option
        optButtons!![mCorrectOption - 1].text = "Globals.aniNames[question.aniID]"
        Glide.with(applicationContext).load(Globals.aniCovers[question.aniID]).transform(roundedCorners).placeholder(circularProgressDrawable).into(optIVs!![mCorrectOption - 1])

        // Show loading text
        tv_question.text = getString(R.string.loading_question)

        // Handle the question type
        when(question.type)
        {
            QType.Theme ->
            {
                // Theme question. Select a correct cover for the theme.
                questionScore = (baseScore * 1.5).toInt()

                if(question.rawID != null)
                {
                    // Using local media file

                    // Build a SimpleExoPlayer
                    exoPlayer = SimpleExoPlayer.Builder(applicationContext).build()
                    exoPlayer?.playWhenReady = false
                    if(preferences!!.getBoolean("tracks_loop", false))
                    {
                        // Loop track option is on
                        exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
                    }

                    // Load raw resource
                    val rds = RawResourceDataSource(applicationContext)
                    rds.open(DataSpec(RawResourceDataSource.buildRawResourceUri(question.rawID)))
                    val mediaSource = ProgressiveMediaSource.Factory{rds}.createMediaSource(MediaItem.fromUri(rds.uri!!))
                    exoPlayer?.setMediaSource(mediaSource)

                    // On ready, start player and timer
                    exoPlayer?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int)
                        {
                            super.onPlaybackStateChanged(state)
                            when (state)
                            {
                                Player.STATE_READY ->
                                {
                                    //Toast.makeText(applicationContext, "Playing", Toast.LENGTH_SHORT).show()
                                    exoPlayer?.play()
                                    startTimer()
                                }
                            }
                        }
                    })

                    exoPlayer?.prepare()
                }
                else if(Globals.themeSrc != ThemeSrc.Local)
                {
                    // Using API

                    // Build a SimpleExoPlayer
                    exoPlayer = SimpleExoPlayer.Builder(applicationContext).build()
                    exoPlayer?.playWhenReady = false

                    // Check preferences
                    if(preferences!!.getBoolean("tracks_loop", false))
                    {
                        // Loop track preference is on
                        if(Globals.themeSrc != ThemeSrc.ATA)
                        {
                            exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
                        }
                    }

                    // On ready, start player and timer
                    exoPlayer?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int)
                        {
                            super.onPlaybackStateChanged(state)
                            when (state)
                            {
                                Player.STATE_READY ->
                                {
                                    //Toast.makeText(applicationContext, "Playing", Toast.LENGTH_SHORT).show()
                                    exoPlayer?.play()
                                    startTimer()
                                }
                                Player.STATE_IDLE ->
                                {
                                    Toast.makeText(applicationContext, "Player failed", Toast.LENGTH_SHORT).show()
                                    tl_imgopts.visibility = View.GONE
                                    showCoverQuestion(question)
                                }
                            }
                        }
                    })

                    // Try to retrieve a theme from API
                    GlobalScope.launch {
                        // Get link to a media file
                        val link = pullLink(question)
                        if(link != null)
                        {

                            // Found a link
                            /*
                            withContext(Dispatchers.Main)
                            {
                                Toast.makeText(applicationContext, "Got link: " + link, Toast.LENGTH_SHORT).show()
                            }
                            */

                            // Load the media file from the link
                            withContext(Dispatchers.Main)
                            {
                                exoPlayer!!.setMediaItem(MediaItem.fromUri(link))
                                exoPlayer!!.prepare()
                            }
                        }
                        else
                        {
                            // No themes found on API
                            // So we pretend this was a cover question
                            runOnUiThread{
                                showCoverQuestion(question)
                            }
                        }
                    }
                }
                else
                {
                    // No local themes found
                    // So we pretend this was a cover question
                    showCoverQuestion(question)
                }
            }
            QType.Cover ->
            {
                // Cover question. Select the correct name for a (mirrored) cover.

                showCoverQuestion(question)
            }
        }
    }

    private fun startTimer()
    {
        // Start countdown while a theme is playing

        // Visuals
        tv_question.text = getString(R.string.theme_question)
        tv_timer.text = "15"
        ll_timer.visibility = View.VISIBLE

        cm_timer.isCountDown = true
        cm_timer.base = SystemClock.elapsedRealtime() + 15000

        cm_timer.onChronometerTickListener = Chronometer.OnChronometerTickListener { chronometer ->
            pb_timer.progress = ((chronometer.base - SystemClock.elapsedRealtime()).toInt() / 1000)
            tv_timer.text = ((chronometer.base - SystemClock.elapsedRealtime()).toInt() / 1000).toString()
            if(chronometer.base <= SystemClock.elapsedRealtime() || exoPlayer == null || !exoPlayer!!.isPlaying)
            {
                // Ran out of time or theme ended
                exoPlayer?.release()
                exoPlayer = null
                chronometer.stop()
                btn_plus.isEnabled = false
                btn_stop.isEnabled = false
                ll_timer.visibility = View.GONE

                // Check preferences
                if(preferences!!.getBoolean("cover_options", false))
                {
                    // Show covers on song end
                    tl_imgopts.visibility = View.VISIBLE
                }
                else
                {
                    // Show text options instead
                    ll_txtopts.visibility = View.VISIBLE
                }

            }
        }
        cm_timer.start()

        // Controls
        btn_plus.setOnClickListener {
            // Pay 1 score for 5 more seconds
            if((cm_timer.base - SystemClock.elapsedRealtime()).toInt() / 1000 < 25)
            {
                cm_timer.base += 5000
                mScore -= 1
            }
        }
        btn_stop.setOnClickListener{
            // Stop the theme, get 1 score for every 5 seconds remaining
            exoPlayer?.release()
            mScore += ((cm_timer.base - SystemClock.elapsedRealtime()).toInt() / 1000) / 5
            cm_timer.base = SystemClock.elapsedRealtime()
            btn_stop.isEnabled = false
        }
        btn_plus.isEnabled = true
        btn_stop.isEnabled = true
    }

    private fun clearOptions()
    {
        // Remove all visual effects from options
        for (option in optButtons!!)
        {
            // Text options
            option.setTextColor(getColor(R.color.light_gray))
            option.typeface = Typeface.DEFAULT
            option.background = ContextCompat.getDrawable(this, R.drawable.default_option_border_bg)
        }
        for (optIV in optIVs!!)
        {
            // Image options
            optIV.alpha = 0.6f
            optIV.colorFilter = null
        }
    }

    override fun onClick(v: View?)
    {
        // Controls for options and submit button
        when(v?.id)
        {
            R.id.tv_option_one, R.id.iv_option_one ->
            {
                if (isConfirmed) return
                selectOption(1)
                selectTVOption(tv_option_one)
                selectIVOption(iv_option_one)
            }
            R.id.tv_option_two, R.id.iv_option_two ->
            {
                if (isConfirmed) return
                selectOption(2)
                selectTVOption(tv_option_two)
                selectIVOption(iv_option_two)
            }
            R.id.tv_option_three, R.id.iv_option_three ->
            {
                if (isConfirmed) return
                selectOption(3)
                selectTVOption(tv_option_three)
                selectIVOption(iv_option_three)
            }
            R.id.tv_option_four, R.id.iv_option_four ->
            {
                if (isConfirmed) return
                selectOption(4)
                selectTVOption(tv_option_four)
                selectIVOption(iv_option_four)
            }
            R.id.btn_submit ->
            {
                if(mSelectedOption == 0 && isConfirmed)
                {
                    // Finishing the current question
                    cm_timer.stop()
                    exoPlayer?.release()
                    exoPlayer = null

                    // Move to the next question
                    mCurrentPosition++

                    when
                    {
                        mCurrentPosition <= mQuestionList!!.size ->
                        {
                            // There are still questions remaining
                            setQuestion()
                        }
                        else ->
                        {
                            // No more questions, show results
                            val intent = Intent(this, ResultActivity::class.java)
                            intent.putExtra(Globals.questionCount, mQuestionList!!.size)
                            intent.putExtra(Globals.score, mScore)
                            intent.putExtra(Globals.baseScore, baseScore)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
                else
                {
                    // Confirming selection
                    isConfirmed = true
                    if(mCorrectOption != mSelectedOption)
                    {
                        // Wrong answer
                        highlightAnswer(mSelectedOption, false)
                        highlightIV(mSelectedOption, getColor(R.color.red))
                        mScore -= questionScore / 2
                    }
                    else
                    {
                        // Correct answer
                        mScore += questionScore
                    }
                    // Highlight correct answer
                    highlightAnswer(mCorrectOption, true)
                    highlightIV(mCorrectOption, getColor(R.color.green))

                    if(mCurrentPosition == mQuestionList!!.size)
                    {
                        // No more questions, next click finishes the game
                        btn_submit.text = getString(R.string.btn_finish)
                    }
                    else
                    {
                        // There are more questions, next click shows the next one
                        btn_submit.text = getString(R.string.btn_next)
                    }
                    mSelectedOption = 0
                }

            }
        }
    }

    private fun highlightAnswer(answer: Int, correct: Boolean)
    {
        // Highlight text option as correct or not
        if(correct)
        {
            optButtons!![answer - 1].background = ContextCompat.getDrawable(this, R.drawable.correct_option_border_bg)
            optButtons!![answer - 1].setTextColor(getColor(R.color.transparent_green))
        }
        else
        {
            optButtons!![answer - 1].background = ContextCompat.getDrawable(this, R.drawable.incorrect_option_border_bg)
            optButtons!![answer - 1].setTextColor(getColor(R.color.transparent_red))
        }
    }

    private fun highlightIV(answer: Int, color: Int)
    {
        // Highlight image option with given color
        optIVs!![answer - 1].alpha = 1f
        optIVs!![answer - 1].setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY)
    }

    private fun selectOption(selectedOption: Int)
    {
        // Select one of the options
        btn_submit.isEnabled = true
        clearOptions()
        mSelectedOption = selectedOption
    }

    private fun selectTVOption(tv: TextView)
    {
        // Highlight text option as selected
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.background = ContextCompat.getDrawable(this, R.drawable.selected_option_border_bg)
    }

    private fun selectIVOption(iv: ImageView)
    {
        // Highlight image as selected
        iv.alpha = 1f
    }

    override fun onStop()
    {
        // Activity stopped, stop player and timer
        super.onStop()
        //Toast.makeText(applicationContext, "Stop", Toast.LENGTH_SHORT).show()
        exoPlayer?.release()
        cm_timer.stop()
        finish()
    }

    private fun showCoverQuestion(question: AniQuestion)
    {
        // Cover question is worth base score
        questionScore = baseScore

        tv_question.text = getString(R.string.cover_question)
        ll_txtopts.visibility = View.VISIBLE
        iv_image.visibility = View.VISIBLE
        Glide.with(applicationContext).load(Globals.aniCovers[question.aniID]).transform(roundedCorners).placeholder(circularProgressDrawable).into(iv_image)
    }

    private suspend fun pullLink(question: AniQuestion) : String?
    {
        return when
        {
            question.link != null -> question.link
            Globals.themeSrc == ThemeSrc.AA -> AAApi.getThemeVideo(question.aniID)
            Globals.themeSrc == ThemeSrc.ATA ->
            {
                ATAApi.getThemeAudio(question.aniID) ?: ATAApi.getThemeVideo(question.aniID)
                /*
                val qLinks = ATAApi.getATATheme(question.aniID, (preferences != null && preferences!!.getString("themes_used", "")!! == "op_only"))
                if(qLinks != null)
                {
                    if(qLinks["audio"] != null)
                    {
                        val audioLink = ATAApi.getATADirectLink(qLinks["audio"] ?: error("ATA: Assertion failed"))
                        // Audio link if possible, else video link
                        audioLink ?: qLinks["video"]
                    }
                    else
                    {
                        // API only returned a video link
                        qLinks["video"]
                    }
                }
                else
                {
                    // API did not return anything
                    null
                }
                */
            }
            // No API used, can't retrieve anything
            else -> null
        }
    }
    private suspend fun pullLinkBG(question: AniQuestion) : String?
    {
        return when
        {
            question.link != null -> question.link
            Globals.themeSrc == ThemeSrc.AA -> AAApi.getThemeVideo(question.aniID)
            Globals.themeSrc == ThemeSrc.ATA ->
            {
                ATAApi.getThemeAudio(question.aniID) ?: ATAApi.getThemeVideo(question.aniID)
                /*
                val qLinks = ATAApi.getATATheme(question.aniID, (preferences != null && preferences!!.getString("themes_used", "")!! == "op_only"))
                if(qLinks != null)
                {
                    if(qLinks["audio"] != null)
                    {
                        val audioLink = ATAApi.getATADirectLink(qLinks["audio"] ?: error("ATA: Assertion failed"))
                        // Try to get audio link
                        audioLink
                    }
                    else
                    {
                        // API did not return an audio link
                        null
                    }
                }
                else
                {
                    // API did not return anything
                    null
                }
                */
            }
            // No API used, can't retrieve anything
            else -> null
        }
    }
}
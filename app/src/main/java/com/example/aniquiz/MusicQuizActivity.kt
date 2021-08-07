package com.example.aniquiz

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.widget.ArrayAdapter
import android.widget.Chronometer
import android.widget.Toast
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.synthetic.main.activity_music_quiz.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


class MusicQuizActivity : AppCompatActivity()
{
    private var preferences: SharedPreferences? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var exoPlayerPreload: SimpleExoPlayer? = null
    private var mediaAPI: MediaAPI? = null
    private var questionID: Int? = null
    private var questionTime: Int = 10000
    private var answerTime: Int = 5000
    private var animeBank: List<Int>? = null
    private var questionCount: Int = 5
    private var questionNum: Int = 0
    private var correctAnswers: Int = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_quiz)

        mediaAPI = ATAApi

        preferences = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

        val adapter = ArrayAdapter<String>(applicationContext, R.layout.spinner_item, Globals.aniNames.values.flatten())
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        actv_title.setAdapter(adapter)

        questionCount = intent.getIntExtra(Globals.questionCount, 5)
        questionTime = intent.getIntExtra(Globals.questionTime, 10) * 1000
        answerTime = intent.getIntExtra(Globals.answerTime, 5) * 1000

        cm_answertimer.isCountDown = true

        tv_title.text = "Loading..."

        CoroutineScope(Main).launch {
            animeBank = mediaAPI!!.getAnimeList()!!.intersect(Globals.aniNames.keys).toList()
            if (animeBank.isNullOrEmpty())
            {
                Toast.makeText(applicationContext, "Empty bank", Toast.LENGTH_SHORT).show()
            }
            initialPhase()
        }

    }

    private fun initialPhase()
    {
        exoPlayerPreload = SimpleExoPlayer.Builder(applicationContext).build().also { exoPlayer ->
            pv_video.player = exoPlayer
        }
        exoPlayerPreload?.playWhenReady = false

        exoPlayerPreload?.addListener(object : Player.Listener
        {
            override fun onPlaybackStateChanged(state: Int)
            {
                super.onPlaybackStateChanged(state)
                when (state)
                {
                    Player.STATE_READY ->
                    {
                        Toast.makeText(applicationContext, "Playing", Toast.LENGTH_SHORT).show()
                        //pv_video.visibility = View.INVISIBLE
                        exoPlayer?.release()
                        exoPlayer = exoPlayerPreload

                        questionPhase()
                    }
                    Player.STATE_IDLE ->
                    {
                        Toast.makeText(applicationContext, "Player failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        questionID = animeBank!!.random()


        CoroutineScope(Main).launch {
            val link = mediaAPI?.getThemeAudio(questionID!!) ?: mediaAPI?.getThemeVideo(questionID!!) ?: "https://animethemes.moe/video/AkuNoHana-OP1.webm"
            withContext(Dispatchers.Main)
            {
                Toast.makeText(applicationContext, "Got link: " + link, Toast.LENGTH_SHORT).show()
            }
            withContext(Dispatchers.Main)
            {
                exoPlayerPreload!!.setMediaItem(MediaItem.fromUri(link))
                exoPlayerPreload!!.prepare()
            }
        }

    }

    private fun questionPhase()
    {
        tv_title.text = "???"
        questionNum++
        actv_title.isEnabled = true
        pv_video.player = exoPlayer
        exoPlayer?.play()

        cm_answertimer.base = SystemClock.elapsedRealtime() + questionTime
        cm_answertimer.onChronometerTickListener = Chronometer.OnChronometerTickListener { chronometer ->
            if (chronometer.base <= SystemClock.elapsedRealtime())
            {
                chronometer.stop()
                answerPhase()
            }
        }
        cm_answertimer.start()
    }

    private fun answerPhase()
    {
        tv_title.text = Globals.aniNames[questionID!!]!![0]

        if (actv_title.text.toString() in Globals.aniNames[questionID!!]!!)
        {
            Toast.makeText(applicationContext, "Correct", Toast.LENGTH_SHORT).show()
            correctAnswers++
        }
        else
        {
            Toast.makeText(applicationContext, "Incorrect", Toast.LENGTH_SHORT).show()
        }
        actv_title.isEnabled = false

        cm_answertimer.base = SystemClock.elapsedRealtime() + answerTime
        cm_answertimer.onChronometerTickListener = Chronometer.OnChronometerTickListener { chronometer ->
            if (chronometer.base <= SystemClock.elapsedRealtime())
            {
                chronometer.stop()
                if(questionNum >= questionCount)
                {
                    endPhase()
                }
                else
                {
                    preloadPhase()
                }
            }
        }
        cm_answertimer.start()

    }

    private fun preloadPhase()
    {
        questionID = animeBank!!.random()
        tv_title.text = "Loading..."
        actv_title.setText("")

        exoPlayerPreload = SimpleExoPlayer.Builder(applicationContext).build()
        exoPlayerPreload?.playWhenReady = false

        exoPlayerPreload?.addListener(object : Player.Listener
        {
            override fun onPlaybackStateChanged(state: Int)
            {
                super.onPlaybackStateChanged(state)
                when (state)
                {
                    Player.STATE_READY ->
                    {
                        Toast.makeText(applicationContext, "Playing", Toast.LENGTH_SHORT).show()
                        //pv_video.visibility = View.INVISIBLE
                        exoPlayer?.release()
                        exoPlayer = exoPlayerPreload

                        questionPhase()
                    }
                    Player.STATE_IDLE ->
                    {
                        Toast.makeText(applicationContext, "Player failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        CoroutineScope(Main).launch {
            val link = mediaAPI?.getThemeAudio(questionID!!) ?: mediaAPI?.getThemeVideo(questionID!!) ?: "https://animethemes.moe/video/AkuNoHana-OP1.webm"
            withContext(Dispatchers.Main)
            {
                Toast.makeText(applicationContext, "Got link: " + link, Toast.LENGTH_SHORT).show()
            }
            withContext(Dispatchers.Main)
            {
                exoPlayerPreload!!.setMediaItem(MediaItem.fromUri(link))
                exoPlayerPreload!!.prepare()
            }
        }
    }

    private fun endPhase()
    {
        tv_title.text = "Finishing..."
        cm_answertimer.base = SystemClock.elapsedRealtime() + 10000
        cm_answertimer.onChronometerTickListener = Chronometer.OnChronometerTickListener { chronometer ->
            if (chronometer.base <= SystemClock.elapsedRealtime())
            {
                chronometer.stop()
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra(Globals.questionCount, questionCount)
                intent.putExtra(Globals.score, correctAnswers)
                startActivity(intent)
                finish()
            }
        }
        cm_answertimer.start()
    }

    override fun onStop()
    {
        super.onStop()
        Toast.makeText(applicationContext, "Stop", Toast.LENGTH_SHORT).show()
        exoPlayer?.release()
        exoPlayerPreload?.release()
        finish()
    }
}
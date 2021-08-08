package com.example.aniquiz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Chronometer
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.synthetic.main.activity_music_quiz.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlin.properties.Delegates


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
    private var questionNum: Int by Delegates.observable(0) { property, oldValue, newValue ->
        // Update shown score whenever it changes
        tv_qnum.text = "" + questionNum + "/" + questionCount
    }
    private var correctAnswers: Int = 0
    private var soundOnly: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_quiz)

        val mediaSrc = intent.getStringExtra(Globals.mediaSrc)
        if(mediaSrc == "Animethemes (audio/video)")
        {
            mediaAPI = ATAApi
        }
        else if (mediaSrc == "Anusic (video)")
        {
            mediaAPI = AAApi
        }
        else
        {
            Toast.makeText(applicationContext, "No media API", Toast.LENGTH_SHORT).show()
            finish()
        }

        if(intent.getBooleanExtra(Globals.soundOnly, false))
        {
            soundOnly = true
        }

        preferences = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

        val adapter = ArrayAdapter<String>(applicationContext, R.layout.spinner_item, Globals.aniNames.values.flatten())
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        actv_title.setAdapter(adapter)

        questionCount = intent.getIntExtra(Globals.questionCount, 5)
        questionTime = intent.getIntExtra(Globals.questionTime, 10) * 1000
        answerTime = intent.getIntExtra(Globals.answerTime, 5) * 1000
        questionNum = 0

        cm_answertimer.isCountDown = true

        tv_title.text = "Loading..."
        pv_video.defaultArtwork = ContextCompat.getDrawable(applicationContext, R.drawable.sound)

        CoroutineScope(Main).launch {
            var mediaBank: List<Int>? = null
            while(mediaBank == null)
            {
                mediaBank = mediaAPI!!.getAnimeList()
                if(mediaBank == null)
                {
                    Toast.makeText(applicationContext, "Failed to get media bank, retrying...", Toast.LENGTH_SHORT).show()
                }
            }
            animeBank = mediaBank.intersect(Globals.aniNames.keys).toList()
            if (animeBank.isNullOrEmpty())
            {
                Toast.makeText(applicationContext, "Failed to find any themes", Toast.LENGTH_SHORT).show()
                finish()
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
            var link: String? = null
            while(link == null)
            {
                if (soundOnly)
                {
                    link = mediaAPI?.getThemeAudio(questionID!!) ?: mediaAPI?.getThemeVideo(questionID!!)
                }
                else
                {
                    link = mediaAPI?.getThemeVideo(questionID!!)
                }
                if(link == null)
                {
                    Toast.makeText(applicationContext, "Failed to get link, retrying...", Toast.LENGTH_SHORT).show()
                }
            }
            withContext(Dispatchers.Main)
            {
                //Toast.makeText(applicationContext, "Got link: " + link, Toast.LENGTH_SHORT).show()
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
        pv_video.visibility = View.INVISIBLE
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
        pv_video.visibility = View.VISIBLE

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
            var link: String? = null
            while(link == null)
            {
                if (soundOnly)
                {
                    link = mediaAPI?.getThemeAudio(questionID!!) ?: mediaAPI?.getThemeVideo(questionID!!)
                }
                else
                {
                    link = mediaAPI?.getThemeVideo(questionID!!)
                }
                if(link == null)
                {
                    Toast.makeText(applicationContext, "Failed to get link, retrying...", Toast.LENGTH_SHORT).show()
                }
            }
            withContext(Dispatchers.Main)
            {
                //Toast.makeText(applicationContext, "Got link: " + link, Toast.LENGTH_SHORT).show()
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
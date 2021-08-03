package com.example.aniquiz

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Chronometer
import android.widget.Toast
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.synthetic.main.activity_aniquestion.*
import kotlinx.android.synthetic.main.activity_music_quiz.*
import kotlinx.android.synthetic.main.activity_quiz_setup.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main

class MusicQuizActivity : AppCompatActivity()
{
    private var preferences: SharedPreferences? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var mediaAPI: MediaAPI? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_quiz)

        mediaAPI = ATAApi

        preferences = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

        var adapter = ArrayAdapter<String>(this, R.layout.spinner_item, Globals.aniNames.values.toList())
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        actv_title.setAdapter(adapter)

        exoPlayer = SimpleExoPlayer.Builder(applicationContext).build().also { exoPlayer ->
            pv_video.player = exoPlayer
        }
        exoPlayer?.playWhenReady = false

        exoPlayer?.addListener(object : Player.Listener
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
                        exoPlayer?.play()
                        cm_answertimer.isCountDown = true
                        cm_answertimer.base = SystemClock.elapsedRealtime() + 15000
                        cm_answertimer.onChronometerTickListener = Chronometer.OnChronometerTickListener { chronometer ->
                            if (chronometer.base <= SystemClock.elapsedRealtime() || exoPlayer == null || !exoPlayer!!.isPlaying)
                            {
                                chronometer.stop()

                                val exoPlayer2 = SimpleExoPlayer.Builder(applicationContext).build()
                                exoPlayer2.playWhenReady = false
                                exoPlayer2.setMediaItem(MediaItem.fromUri("https://animethemes.moe/video/AkuNoHana-OP1.webm"))

                                exoPlayer2.addListener(object : Player.Listener
                                {
                                    override fun onPlaybackStateChanged(state: Int)
                                    {
                                        super.onPlaybackStateChanged(state)
                                        when (state)
                                        {
                                            Player.STATE_READY ->
                                            {
                                                Toast.makeText(applicationContext, "Next", Toast.LENGTH_SHORT).show()
                                                pv_video.player = exoPlayer2
                                                exoPlayer2.play()
                                                exoPlayer?.release()
                                            }
                                        }
                                    }
                                })
                                exoPlayer2.prepare()
                                //pv_video.visibility = View.VISIBLE
                            }
                        }
                        cm_answertimer.start()
                    }
                    Player.STATE_IDLE ->
                    {
                        Toast.makeText(applicationContext, "Player failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        val aniID = Globals.aniNames.keys.random()
        tv_title.text = Globals.aniNames[aniID]

        CoroutineScope(Main).launch {
            //val link = mediaAPI?.getThemeAudio(aniID) ?: mediaAPI?.getThemeVideo(aniID) ?: "https://animethemes.moe/video/AkuNoHana-OP1.webm"
            val link = mediaAPI?.getThemeVideo(aniID) ?: "https://animethemes.moe/video/AkuNoHana-OP1.webm"
            exoPlayer?.addMediaItem(MediaItem.fromUri("https://animethemes.moe/video/AkuNoHana-OP1.webm"))
            withContext(Dispatchers.Main)
            {
                Toast.makeText(applicationContext, "Got link: " + link, Toast.LENGTH_SHORT).show()
            }
            withContext(Dispatchers.Main)
            {
                exoPlayer!!.setMediaItem(MediaItem.fromUri(link))
                exoPlayer!!.prepare()
            }
        }
    }

    override fun onStop()
    {
        super.onStop()
        Toast.makeText(applicationContext, "Stop", Toast.LENGTH_SHORT).show()
        exoPlayer?.release()
        finish()
    }
}
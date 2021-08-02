package com.example.aniquiz

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    private var mediaAPI: MediaAPI? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_quiz)

        mediaAPI = ATAApi

        preferences = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

        exoPlayer = SimpleExoPlayer.Builder(applicationContext).build()
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
                        //Toast.makeText(applicationContext, "Playing", Toast.LENGTH_SHORT).show()
                        exoPlayer?.play()
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
            val link = mediaAPI?.getThemeAudio(aniID) ?: mediaAPI?.getThemeVideo(aniID) ?: "https://animethemes.moe/video/AkuNoHana-OP1.webm"
            withContext(Dispatchers.Main)
            {
                exoPlayer!!.setMediaItem(MediaItem.fromUri(link))
                exoPlayer!!.prepare()
            }
        }
    }
}
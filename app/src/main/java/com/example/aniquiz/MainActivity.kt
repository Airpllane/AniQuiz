package com.example.aniquiz

import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Main menu - 2 buttons
        btn_aniquiz.setOnClickListener{
            //startActivity(Intent(this, QuizSetupActivity::class.java))
        }
        btn_musquiz.setOnClickListener{
            startActivity(Intent(this, MusicQuizActivity::class.java))
        }
        btn_settings.setOnClickListener{
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
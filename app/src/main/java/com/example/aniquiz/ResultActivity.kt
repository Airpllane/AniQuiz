package com.example.aniquiz

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val questionCount = intent.getIntExtra(Globals.questionCount, 0)
        val score = intent.getIntExtra(Globals.score, 0)
        val baseScore = intent.getIntExtra(Globals.baseScore, 5)
        val highScore = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE).getInt("high_score", 0)

        if(score <= highScore)
        {
            // Old high score

            tv_new_high_score.visibility = View.GONE
        }
        else
        {
            // New high score

            val prefs = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)
            with(prefs.edit())
            {
                // Save high score to SharedPreferences
                putInt("high_score", score)
                apply()
            }
            tv_new_high_score.visibility = View.VISIBLE
        }

        tv_congrats.text = when
        {
            score >= questionCount * baseScore * 1.2 -> "Great!"
            score >= questionCount * baseScore * 1 -> "Good"
            score >= questionCount * baseScore * 0.8 -> "Fine"
            score >= questionCount * baseScore * 0.6 -> "Not good"
            else -> "Terrible"
        }
        tv_score.text = getString(R.string.score_counter, score)
        tv_high_score.text = getString(R.string.high_score_counter, highScore)
        btn_finish.setOnClickListener{
            finish()
        }
    }
}
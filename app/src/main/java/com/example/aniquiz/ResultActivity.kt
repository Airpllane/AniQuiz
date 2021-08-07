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



        tv_congrats.text = "Result:"
        tv_score.text = "" + score + "/" + questionCount
        btn_finish.setOnClickListener{
            finish()
        }
    }
}
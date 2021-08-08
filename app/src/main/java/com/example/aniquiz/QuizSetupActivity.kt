package com.example.aniquiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_quiz_setup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizSetupActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_setup)

        // Array of anime sources
        val spnArray = arrayListOf("MAL Top 10 (Local)")
        if (getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE).getBoolean("mal_sync", false) &&
            !getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE).getString("mal_access_token", null).isNullOrEmpty())
        {
            // MAL sync is on
            spnArray.add("My List (MAL)")
            spnArray.add("Top Anime (MAL)")
        }
        spn_src_type.onItemSelectedListener = object : OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                btn_start_quiz.isEnabled = false
                when(parent!!.getItemAtPosition(position).toString())
                {
                    "MAL Top 10 (Local)" ->
                    {
                        // Use only local data
                        Globals.syncToLocal()
                        btn_start_quiz.isEnabled = true
                    }
                    "My List (MAL)" ->
                    {
                        // Try to connect to MAL, wait for answer
                        GlobalScope.launch {
                            if(Globals.syncToMyMAL(getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE).getString("sync_amt", "100")!!.toInt()))
                            {
                                // Connection successful
                                if(Globals.aniNames.size > 4)
                                {
                                    // Enough anime, enable the button
                                    runOnUiThread {
                                        btn_start_quiz.isEnabled = true
                                    }
                                }
                                else
                                {
                                    // Not enough anime, alert user
                                    withContext(Dispatchers.Main)
                                    {
                                        Toast.makeText(applicationContext, "At least 5 anime is required for a quiz", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            else
                            {
                                // Connection failed, alert user
                                withContext(Dispatchers.Main)
                                {
                                    Toast.makeText(applicationContext, "Failed to connect to MAL", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    "Top Anime (MAL)" ->
                    {
                        // Try to connect to MAL, wait for answer
                        GlobalScope.launch {
                            if(Globals.syncToTopMAL(getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE).getString("sync_amt", "100")!!.toInt()))
                            {
                                // Connection successful
                                if(Globals.aniNames.size > 4)
                                {
                                    // Enough anime, enable the button
                                    runOnUiThread {
                                        btn_start_quiz.isEnabled = true
                                    }
                                }
                                else
                                {
                                    // Not enough anime, alert user
                                    withContext(Dispatchers.Main)
                                    {
                                        Toast.makeText(applicationContext, "At least 5 anime is required for a quiz", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            else
                            {
                                // Connection failed
                                withContext(Dispatchers.Main)
                                {
                                    Toast.makeText(applicationContext, "Failed to connect to MAL", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?)
            {
                Toast.makeText(applicationContext, "Nothing selected", Toast.LENGTH_SHORT).show()
            }
        }
        var adapter = ArrayAdapter<String>(this, R.layout.spinner_item, spnArray)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spn_src_type.adapter = adapter

        // Array of music sources
        val spnOpArray = arrayOf("Animethemes (audio/video)", "Anusic (video)")
        adapter = ArrayAdapter<String>(this, R.layout.spinner_item, spnOpArray)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spn_src_music.adapter = adapter

        // Set min and max total questions
        np_question_amt.minValue = 1
        np_question_amt.maxValue = 20

        np_question_time.minValue = 5
        np_question_time.maxValue = 30

        np_answer_time.minValue = 5
        np_answer_time.maxValue = 30

        btn_start_quiz.setOnClickListener {
            // Start quiz with selected settings

            val intent = Intent(this, MusicQuizActivity::class.java)
            intent.putExtra(Globals.questionCount, np_question_amt.value)
            intent.putExtra(Globals.questionTime, np_question_time.value)
            intent.putExtra(Globals.answerTime, np_answer_time.value)
            intent.putExtra(Globals.mediaSrc, spn_src_music.selectedItem.toString())
            intent.putExtra(Globals.soundOnly, cb_soundonly.isChecked)

            startActivity(intent)
            finish()
        }
    }

}

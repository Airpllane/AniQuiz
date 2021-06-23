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
        val spnArray = arrayListOf("MAL Top 10 (Local)", "MAL Top 50 (Jikan)")
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
                    "MAL Top 50 (Jikan)" ->
                    {
                        // Try to connect to Jikan, wait for answer
                        GlobalScope.launch {
                            if(Globals.syncToJikan())
                            {
                                // Connection successful, enable the button
                                runOnUiThread {
                                    btn_start_quiz.isEnabled = true
                                }
                            }
                            else
                            {
                                // Connection failed, alert user
                                withContext(Dispatchers.Main)
                                {
                                    Toast.makeText(applicationContext, "Failed to connect to Jikan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
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
        val spnOpArray = arrayOf("Local files", "Music API (fast)", "Video API (slow)")
        spn_src_music.onItemSelectedListener = object : OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                when(parent!!.getItemAtPosition(position).toString())
                {
                    "Local files" ->
                    {
                        // Use only local data
                        Globals.themeSrc = ThemeSrc.Local
                    }
                    "Music API (fast)" ->
                    {
                        // Use animethemes-api - faster, music and videos
                        Globals.themeSrc = ThemeSrc.ATA
                    }
                    "Video API (slow)" ->
                    {
                        // Use anusic-api - slow, videos only
                        Globals.themeSrc = ThemeSrc.AA
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?)
            {
                Toast.makeText(applicationContext, "Nothing selected", Toast.LENGTH_SHORT).show()
            }
        }
        adapter = ArrayAdapter<String>(this, R.layout.spinner_item, spnOpArray)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spn_src_music.adapter = adapter

        // Set min and max total questions
        np_question_amt.minValue = 1
        np_question_amt.maxValue = 20

        btn_start_quiz.setOnClickListener {
            // Start quiz with selected settings
            //Toast.makeText(applicationContext, "Starting with " + spn_src_type.selectedItem, Toast.LENGTH_SHORT).show()
            val questionList = Globals.getAniQuestions(np_question_amt.value)
            val intent = Intent(this, AniQuestionActivity::class.java)
            intent.putExtra(Globals.userName, "QuizSetupUserName")
            intent.putExtra(Globals.questionList, questionList)
            startActivity(intent)
            finish()
        }
    }
}
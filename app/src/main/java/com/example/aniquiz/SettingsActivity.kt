package com.example.aniquiz

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null)
        {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat()
    {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
        {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val mal_token = findPreference<Preference>("mal_token")
            mal_token!!.setOnPreferenceClickListener {
                // Button to retrieve MAL token
                startActivity(Intent(GlobalContext.appContext!!, AuthActivity::class.java))
                true
            }
            /* It seems that MAL does not support refreshing tokens
            val mal_refresh = findPreference<Preference>("mal_refresh")
            mal_refresh!!.setOnPreferenceClickListener {
                // Button to refresh MAL token
                GlobalScope.launch {
                    Looper.prepare()
                    if(MALApi.refreshToken())
                    {
                        Toast.makeText(GlobalContext.appContext!!, "MAL token refreshed", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        Toast.makeText(GlobalContext.appContext!!, "Failed to refresh MAL token", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            */
            val sync_amt = findPreference<EditTextPreference>("sync_amt")
            sync_amt!!.setOnBindEditTextListener {
                // Restrict EditText to numbers
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
            sync_amt.setOnPreferenceChangeListener {preference, newValue ->
                // Must be in range 5 - 500
                if(newValue.toString().toInt() in 5..500)
                {
                    true
                }
                else
                {
                    Toast.makeText(GlobalContext.appContext!!, "You can pull 5 to 500 entries", Toast.LENGTH_SHORT).show()
                    false
                }

            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when(item.itemId)
        {
            R.id.home ->
            {
                finish()
            }
        }
        return false
    }
}
package com.example.aniquiz

import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AAApi : MediaAPI
{
    override suspend fun getThemeAudio(ID: Int) = suspendCoroutine<String?> { continuation ->
        continuation.resume(null)
    }

    override suspend fun getThemeVideo(ID: Int) = suspendCoroutine<String?> { continuation ->
        val stringRequest = StringRequest(
            Request.Method.GET, "https://anusic-api.herokuapp.com/api/v1/anime/" + ID,
            Response.Listener { response ->
                val jsonArray = JSONObject(response).getJSONObject("data").getJSONArray("collections").getJSONObject(0).getJSONArray("themes")
                val themeList = mutableListOf<String>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i)
                    themeList.add(node.getJSONArray("sources").getJSONObject(0).getString("link"))
                }
                if (!themeList.isEmpty())
                {
                    // Return a link to one random theme from this anime
                    continuation.resume(themeList.random())
                }
                else
                {
                    // Failed to fill themeList
                    continuation.resume(null)
                }
            },
            Response.ErrorListener { error ->
                // AA did not respond correctly
                continuation.resume(null)
            })
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    override suspend fun getAnimeList() = suspendCoroutine<List<Int>?> { continuation ->
        val stringRequest = object : StringRequest(
            Method.GET, "https://anusic-api.herokuapp.com/api/v1/anime",
            Response.Listener { response ->
                val jsonArray = JSONObject(response).getJSONArray("data")
                val animeList = mutableListOf<Int>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i)
                    animeList.add(node.getInt("id"))
                }
                if (!animeList.isEmpty())
                {
                    // Return all existing anime
                    continuation.resume(animeList.toList())
                }
                else
                {
                    // Failed to fill animeList
                    continuation.resume(null)
                }
            },
            Response.ErrorListener { error ->
                // AA did not respond correctly
                continuation.resume(null)
            })
        {}
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    /*
    suspend fun getAATheme(ID: Int, opOnly: Boolean) = suspendCoroutine<String?> { continuation ->
        // Retrieve a link to theme (video)
        val stringRequest = StringRequest(
            Request.Method.GET, "https://anusic-api.herokuapp.com/api/v1/anime/" + ID,
            Response.Listener { response ->
                val jsonArray = JSONObject(response).getJSONObject("data").getJSONArray("collections").getJSONObject(0).getJSONArray("themes")
                val opList = mutableListOf<String>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i)
                    if(!opOnly || node.getInt("type") == 0)
                    {
                        opList.add(node.getJSONArray("sources").getJSONObject(0).getString("link"))
                    }
                }
                if(!opList.isEmpty())
                {
                    // Return a link to one random theme from this anime
                    continuation.resume(opList.random())
                }
                else
                {
                    // Failed to fill opList
                    continuation.resume(null)
                }
            },
            Response.ErrorListener { error ->
                // AA did not respond correctly
                continuation.resume(null)
            })
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    suspend fun getAAOpList() = suspendCoroutine<List<Int>?> { continuation ->
        // Get all existing AA entries
        val stringRequest = object : StringRequest(
            Method.GET, "https://anusic-api.herokuapp.com/api/v1/anime",
            Response.Listener { response ->
                val jsonArray = JSONObject(response).getJSONArray("data")
                val opList = mutableListOf<Int>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i)
                    opList.add(node.getInt("id"))
                }
                if(!opList.isEmpty())
                {
                    // Return all existing anime
                    continuation.resume(opList.toList())
                }
                else
                {
                    // Failed to fill opList
                    continuation.resume(null)
                }
            },
            Response.ErrorListener { error ->
                // AA did not respond correctly
                continuation.resume(null)
            }){}
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }
    */
}
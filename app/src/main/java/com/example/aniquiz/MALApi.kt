package com.example.aniquiz

import android.content.Context
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object MALApi : DataAPI
{
    const val client_id = "cbb6efcaadfa78638ad7456d1d5a95dd"

    override suspend fun getAnimeList(amt: Int) = suspendCoroutine<Map<Int, Map<String, List<String>>>?> { continuation ->
        val url = "https://api.myanimelist.net/v2/anime/ranking?fields=alternative_titles&ranking_type=all&limit=$amt"
        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                val jsonArray = JSONObject(response).getJSONArray("data")
                val aniMap = mutableMapOf<Int, MutableMap<String, MutableList<String>>>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i).getJSONObject("node")
                    aniMap.put(node.getInt("id"), mutableMapOf("titles" to mutableListOf(), "covers" to mutableListOf()))
                    aniMap[node.getInt("id")]!!["titles"]!!.add(node.getString("title"))
                    aniMap[node.getInt("id")]!!["covers"]!!.add(node.getJSONObject("main_picture").getString("medium"))
                    val alternativeTitles = node.getJSONObject("alternative_titles")
                    val synonyms = alternativeTitles.getJSONArray("synonyms")
                    for (i in 0 until synonyms.length())
                    {
                        aniMap[node.getInt("id")]!!["titles"]!!.add(synonyms.getString(i))
                    }
                    aniMap[node.getInt("id")]!!["titles"]!!.add(alternativeTitles.getString("en"))
                    aniMap[node.getInt("id")]!!["titles"]!!.add(alternativeTitles.getString("ja"))
                    aniMap[node.getInt("id")]!!["titles"]!!.removeIf(String::isEmpty)
                    aniMap[node.getInt("id")]!!["titles"] = aniMap[node.getInt("id")]!!["titles"]!!.distinct().toMutableList()
                }
                continuation.resume(aniMap.toMap())
            },
            Response.ErrorListener { error ->
                continuation.resume(null)
            })
        {
            override fun getHeaders(): MutableMap<String, String>
            {
                return mutableMapOf("Authorization" to getMALAuthHeader())
            }
        }
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    suspend fun getUserAnimeList(amt: Int) = suspendCoroutine<Map<Int, Map<String, List<String>>>?> { continuation ->
        val url = "https://api.myanimelist.net/v2/users/@me/animelist?fields=alternative_titles&status=completed&sort=list_score&limit=$amt"
        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                val jsonArray = JSONObject(response).getJSONArray("data")
                val aniMap = mutableMapOf<Int, MutableMap<String, MutableList<String>>>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i).getJSONObject("node")
                    aniMap.put(node.getInt("id"), mutableMapOf("titles" to mutableListOf(), "covers" to mutableListOf()))
                    aniMap[node.getInt("id")]!!["titles"]!!.add(node.getString("title"))
                    aniMap[node.getInt("id")]!!["covers"]!!.add(node.getJSONObject("main_picture").getString("medium"))
                    val alternativeTitles = node.getJSONObject("alternative_titles")
                    val synonyms = alternativeTitles.getJSONArray("synonyms")
                    for (i in 0 until synonyms.length())
                    {
                        aniMap[node.getInt("id")]!!["titles"]!!.add(synonyms.getString(i))
                    }
                    aniMap[node.getInt("id")]!!["titles"]!!.add(alternativeTitles.getString("en"))
                    aniMap[node.getInt("id")]!!["titles"]!!.add(alternativeTitles.getString("ja"))
                    aniMap[node.getInt("id")]!!["titles"] = aniMap[node.getInt("id")]!!["titles"]!!.distinct().toMutableList()
                }
                continuation.resume(aniMap.toMap())
            },
            Response.ErrorListener { error ->
                continuation.resume(null)
            })
        {
            override fun getHeaders(): MutableMap<String, String>
            {
                return mutableMapOf("Authorization" to getMALAuthHeader())
            }
        }
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    suspend fun getUserName() = suspendCoroutine<String> { continuation ->
        val stringRequest = object : StringRequest(
            Request.Method.GET, "https://api.myanimelist.net/v2/users/@me",
            Response.Listener { response ->
                continuation.resume(JSONObject(response).getString("name"))
            },
            Response.ErrorListener { error ->
                continuation.resume("Error" + error.networkResponse.statusCode)
            })
        {
            override fun getHeaders(): MutableMap<String, String>
            {
                return mutableMapOf("Authorization" to getMALAuthHeader())
            }
        }
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    suspend fun refreshToken() = suspendCoroutine<Boolean> { continuation ->
        val stringRequest = object : StringRequest(
            Request.Method.POST, "https://myanimelist.net/v1/oauth2/token",
            Response.Listener { response ->
                val jsonResponse = JSONObject(response)
                val prefs = GlobalContext.appContext!!.getSharedPreferences(GlobalContext.appContext!!.packageName + "_preferences", Context.MODE_PRIVATE)
                with(prefs.edit())
                {
                    // Save tokens to SharedPreferences
                    putString("mal_token_type", jsonResponse.getString("token_type"))
                    putString("mal_access_token", jsonResponse.getString("access_token"))
                    putString("mal_refresh_token", jsonResponse.getString("refresh_token"))
                    apply()
                }
                continuation.resume(true)
            },
            Response.ErrorListener { error ->
                continuation.resume(false)
            })
        {
            override fun getHeaders(): MutableMap<String, String>
            {
                return mutableMapOf("client_id" to client_id, "grant_type" to "refresh_token", "refresh_token" to GlobalContext.appContext!!.getSharedPreferences(GlobalContext.appContext!!.packageName + "_preferences", Context.MODE_PRIVATE).getString("mal_refresh_token", "none")!!)
            }
        }
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    fun getMALAuthHeader(): String
    {
        return GlobalContext.appContext!!.getSharedPreferences(GlobalContext.appContext!!.packageName + "_preferences", Context.MODE_PRIVATE).getString("mal_token_type", "none")!! +
                " " +
                GlobalContext.appContext!!.getSharedPreferences(GlobalContext.appContext!!.packageName + "_preferences", Context.MODE_PRIVATE).getString("mal_access_token", "none")
    }
}
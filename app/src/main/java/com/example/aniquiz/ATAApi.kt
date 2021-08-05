package com.example.aniquiz

import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object ATAApi : MediaAPI
{
    override suspend fun getThemeAudio(ID: Int): String?
    {
        val audioLink = generateLink(ID, LinkType.Audio)
        if (audioLink == null) return null
        else
        {
            return suspendCoroutine<String?> { continuation ->
                val audioLinkS = audioLink.replace("http://", "https://")
                val stringRequest = StringRequest(
                    Request.Method.GET, audioLinkS,
                    Response.Listener { response ->
                        val directLink = JSONObject(response).getString("audio")
                        continuation.resume(directLink)
                    },
                    Response.ErrorListener { error ->
                        // ATA did not respond correctly
                        continuation.resume(null)
                    })
                stringRequest.retryPolicy = DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
                RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
            }
        }
    }

    override suspend fun getThemeVideo(ID: Int): String?
    {
        return generateLink(ID, LinkType.Video)
    }

    override suspend fun getAnimeList(): List<Int>? = suspendCoroutine<List<Int>?> { continuation ->
        // Get all existing ATA entries
        val stringRequest = object : StringRequest(
            Method.GET, "https://animethemes-api.herokuapp.com/api/v1/list/anime",
            Response.Listener { response ->
                val jsonArray = JSONArray(response)
                val animeList = mutableListOf<Int>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i)
                    if (node.getJSONArray("themes").length() > 0 && node.getJSONArray("themes").getJSONObject(0).getJSONArray("mirrors").length() > 0)
                    {
                        animeList.add(node.getInt("mal_id"))
                    }
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
                // ATA did not respond correctly
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

    private enum class LinkType
    {
        Audio, Video
    }

    private suspend fun generateLink(ID: Int, Type: LinkType): String? = suspendCoroutine<String?> { continuation ->
        val stringRequest = StringRequest(
            Request.Method.GET, "https://animethemes-api.herokuapp.com/api/v1/anime/" + ID,
            Response.Listener { response ->
                try
                {
                    val jsonArray = JSONObject(response).getJSONArray("themes")
                    val themeList = mutableListOf<String>()
                    //val themeMapList = mutableListOf<Map<String, String>>()
                    for (i in 0 until jsonArray.length())
                    {
                        val node: JSONObject = jsonArray.getJSONObject(i)
                        val audioQLink = node.getJSONArray("mirrors").getJSONObject(0).getString("audio")
                        val videoLink = node.getJSONArray("mirrors").getJSONObject(0).getString("mirror")

                        themeList.add(
                            when (Type)
                            {
                                LinkType.Audio -> audioQLink
                                LinkType.Video -> videoLink
                            }
                        )
                    }
                    if (themeList.isNotEmpty())
                    {
                        // Return a link to one random theme from this anime
                        continuation.resume(themeList.random())
                    }
                    else
                    {
                        // Failed to fill themeList
                        continuation.resume(null)
                    }
                }
                catch (e: Exception)
                {
                    // ATA does not have this anime
                    continuation.resume(null)
                }
            },
            Response.ErrorListener { error ->
                // ATA did not respond correctly
                continuation.resume(null)
            })
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    /*
    suspend fun getATATheme(ID: Int, opOnly: Boolean) = suspendCoroutine<Map<String, String>?> { continuation ->
        // Retrieve a (non-direct) link to theme
        val stringRequest = StringRequest(
            Request.Method.GET, "https://animethemes-api.herokuapp.com/api/v1/anime/" + ID,
            Response.Listener { response ->
                try
                {
                    val jsonArray = JSONObject(response).getJSONArray("themes")
                    val themeList = mutableListOf<String>()
                    val themeMapList = mutableListOf<Map<String, String>>()
                    for (i in 0 until jsonArray.length())
                    {
                        val node: JSONObject = jsonArray.getJSONObject(i)
                        if(!opOnly || node.getString("type").contains("OP", true))
                        {
                            val audioQLink = node.getJSONArray("mirrors").getJSONObject(0).getString("audio")
                            val videoLink = node.getJSONArray("mirrors").getJSONObject(0).getString("mirror")
                            themeList.add(audioQLink)
                            themeMapList.add(mapOf("audio" to audioQLink, "video" to videoLink))
                        }
                    }
                    if (themeList.isNotEmpty())
                    {
                        // Return a link to one random theme from this anime
                        continuation.resume(themeMapList.random())
                    }
                    else
                    {
                        // Failed to fill opList
                        continuation.resume(null)
                    }
                }
                catch (e: Exception)
                {
                    // ATA does not have this anime
                    continuation.resume(null)
                }
            },
            Response.ErrorListener { error ->
                // ATA did not respond correctly
                continuation.resume(null)
            })
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    suspend fun getATADirectLink(audioLink: String) = suspendCoroutine<String?> { continuation ->
        // Convert a non-direct link to direct link
        val audioLinkS = audioLink.replace("http://", "https://")
        val stringRequest = StringRequest(
            Request.Method.GET, audioLinkS,
            Response.Listener { response ->
                val directLink = JSONObject(response).getString("audio")
                continuation.resume(directLink)
            },
            Response.ErrorListener { error ->
                // ATA did not respond correctly
                continuation.resume(null)
            })
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        RequestClass.getInstance(GlobalContext.appContext!!).addToRequestQueue(stringRequest)
    }

    suspend fun getATAOpList() = suspendCoroutine<List<Int>?> { continuation ->
        // Get all existing ATA entries
        val stringRequest = object : StringRequest(
            Method.GET, "https://animethemes-api.herokuapp.com/api/v1/list/anime",
            Response.Listener { response ->
                val jsonArray = JSONArray(response)
                val opList = mutableListOf<Int>()
                for (i in 0 until jsonArray.length())
                {
                    val node: JSONObject = jsonArray.getJSONObject(i)
                    if(node.getJSONArray("themes").length() > 0 && node.getJSONArray("themes").getJSONObject(0).getJSONArray("mirrors").length() > 0)
                    {
                        opList.add(node.getInt("mal_id"))
                    }
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
                // ATA did not respond correctly
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
    */
}
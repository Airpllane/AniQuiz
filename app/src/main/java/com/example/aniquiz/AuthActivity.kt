package com.example.aniquiz

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kotlinx.android.synthetic.main.activity_auth.*
import org.json.JSONObject
import java.util.*

class AuthActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        wv_auth.settings.javaScriptEnabled = true

        // Set necessary values for OAuth
        val client_id = "cbb6efcaadfa78638ad7456d1d5a95dd"
        val code_challenge = PKCEUtil.generateCodeVerifier()
        val state = UUID.randomUUID().toString()
        val rurl = "http://localhost/oauth"

        // First stage
        val uri = Uri.parse("https://myanimelist.net/v1/oauth2/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", client_id)
            .appendQueryParameter("code_challenge", code_challenge)
            .appendQueryParameter("state", state)
            .build()

        wv_auth.webViewClient = object : WebViewClient()
        {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean
            {
                if (request?.url.toString().startsWith(rurl))
                {
                    val responseState = request?.url?.getQueryParameter("state")
                    if (responseState == state)
                    {
                        // Second stage
                        val stringRequest = object : StringRequest(Request.Method.POST, "https://myanimelist.net/v1/oauth2/token",
                            Response.Listener { response ->
                                val jsonResponse = JSONObject(response)
                                val prefs = getSharedPreferences(applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)
                                with(prefs.edit())
                                {
                                    // Save tokens to SharedPreferences
                                    putString("mal_token_type", jsonResponse.getString("token_type"))
                                    putString("mal_access_token", jsonResponse.getString("access_token"))
                                    putString("mal_refresh_token", jsonResponse.getString("refresh_token"))
                                    apply()
                                }
                                finish()
                            },
                            Response.ErrorListener { error ->
                                Toast.makeText(applicationContext, "error " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show()
                                finish()
                            })
                        {
                            override fun getParams(): MutableMap<String, String>?
                            {
                                return mutableMapOf("client_id" to client_id, "code" to request.url.getQueryParameter("code")!!, "code_verifier" to code_challenge, "grant_type" to "authorization_code")
                            }
                        }
                        RequestClass.getInstance(applicationContext).addToRequestQueue(stringRequest)
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        wv_auth.loadUrl(uri.toString())
    }
}
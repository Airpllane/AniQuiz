package com.example.aniquiz

import android.util.Base64
import java.security.SecureRandom

object PKCEUtil
{
    fun generateCodeVerifier(): String
    {
        val sr = SecureRandom()
        val code = ByteArray(64)
        sr.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
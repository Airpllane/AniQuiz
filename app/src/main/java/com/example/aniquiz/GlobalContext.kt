package com.example.aniquiz

import android.app.Application
import android.content.Context

class GlobalContext : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        appContext = applicationContext
    }

    companion object
    {
        var appContext: Context? = null
            private set
    }
}
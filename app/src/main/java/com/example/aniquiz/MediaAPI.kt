package com.example.aniquiz

interface MediaAPI
{
    suspend fun getThemeAudio(ID: Int): String?
    suspend fun getThemeVideo(ID: Int): String?
    suspend fun getAnimeList(): List<Int>?
}
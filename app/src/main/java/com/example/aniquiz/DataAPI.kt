package com.example.aniquiz

interface DataAPI
{
    suspend fun getAnimeList(amt: Int): Map<Int, Map<String, List<String>>>?
}
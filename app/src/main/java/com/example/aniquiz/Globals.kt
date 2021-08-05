package com.example.aniquiz

import kotlin.random.Random

object Globals
{
    // Strings for intent extras
    const val userName: String = "User"
    const val questionCount: String = "QuestionCount"
    const val score: String = "Score"
    const val questionList: String = "QuestionList"
    const val baseScore: String = "BaseScore"

    // Local data
    private val localAniNames = mapOf(
        20 to listOf("Naruto"),
        1535 to listOf("Death Note"),
        5114 to listOf("Fullmetal Alchemist: Brotherhood"),
        9253 to listOf("Steins;Gate"),
        11757 to listOf("Sword Art Online"),
        16498 to listOf("Shingeki no Kyojin", "Attack on Titan"),
        22319 to listOf("Tokyo Ghoul"),
        30276 to listOf("One Punch Man"),
        31964 to listOf("Boku no Hero Academia"),
        32281 to listOf("Kimi no Na wa.")
    )
    private val localAniCovers = mapOf(
        20 to "https://cdn.myanimelist.net/images/anime/13/17405l.webp",
        1535 to "https://cdn.myanimelist.net/images/anime/9/9453l.webp",
        5114 to "https://cdn.myanimelist.net/images/anime/1223/96541l.webp",
        9253 to "https://cdn.myanimelist.net/images/anime/5/73199l.webp",
        11757 to "https://cdn.myanimelist.net/images/anime/11/39717l.webp",
        16498 to "https://cdn.myanimelist.net/images/anime/10/47347l.webp",
        22319 to "https://cdn.myanimelist.net/images/anime/5/64449l.webp",
        30276 to "https://cdn.myanimelist.net/images/anime/12/76049l.webp",
        31964 to "https://cdn.myanimelist.net/images/anime/10/78745l.webp",
        32281 to "https://cdn.myanimelist.net/images/anime/5/87048l.webp"
    )
    // Current values, create as local
    var aniNames = localAniNames
    var aniCovers = localAniCovers
    var themeSrc = ThemeSrc.Local

    fun getBackgrounds(): List<Int>?
    {
        // Collect all valid raw music files
        val fields = R.drawable::class.java.declaredFields
        val BGs = mutableListOf<Int>()
        for (i in fields.indices)
        {
            if(fields[i].name.startsWith("bg_"))
            {
                BGs.add(fields[i].getInt(fields[i]))
            }
        }
        return BGs.toList()
    }

    private fun getLocalOps(): Map<Int, List<Int>>?
    {
        // Collect all valid raw music files
        val fields = R.raw::class.java.declaredFields
        val OPs = mutableMapOf<Int, MutableList<Int>>()
        for (i in fields.indices)
        {
            if(fields[i].name.contains("op_"))
            {
                val num = fields[i].name.split("_")[1].toInt()
                if(num in OPs)
                {
                    OPs.get(num)!!.add(fields[i].getInt(fields[i]))
                }
                else
                {
                    OPs.put(num, mutableListOf(fields[i].getInt(fields[i])))
                }
            }
        }
        return OPs.toMap()
    }

    fun syncToLocal()
    {
        // Set current anime list and covers to local
        aniNames = localAniNames
        aniCovers = localAniCovers
    }
/*
    suspend fun syncToJikan() : Boolean
    {
        // Set current anime list and covers to MAL top 50 (pulled from Jikan)
        val jikanResponse = MALApi.getTop50Anime()
        if(jikanResponse != null)
        {
            aniNames = jikanResponse.map { (key, value) ->
                key to value.getOrDefault("title", "(Missing title)")
            }.toMap()
            aniCovers = jikanResponse.map { (key, value) ->
                key to value.getOrDefault("cover", "https://banner2.cleanpng.com/20180402/tww/kisspng-emoji-iphone-text-messaging-sms-no-symbol-no-5ac1fb2a90a786.7966473015226621865925.jpg")
            }.toMap()
            return true
        }
        else
        {
            // Failed
            return false
        }
    }

    suspend fun syncToMyMAL(amt: Int) : Boolean
    {
        // Set current anime list and covers to user's completed MAL anime
        val MALResponse = MALApi.getCompletedAnime(amt)
        if(MALResponse != null)
        {
            aniNames = MALResponse.map { (key, value) ->
                key to value.getOrDefault("title", "(Missing title)")
            }.toMap()
            aniCovers = MALResponse.map { (key, value) ->
                key to value.getOrDefault("cover", "https://banner2.cleanpng.com/20180402/tww/kisspng-emoji-iphone-text-messaging-sms-no-symbol-no-5ac1fb2a90a786.7966473015226621865925.jpg")
            }.toMap()
            return true
        }
        else
        {
            // Failed
            return false
        }
    }

    suspend fun syncToTopMAL(amt: Int) : Boolean
    {
        // Set current anime list and covers to MAL's "Top anime" list
        val MALResponse = MALApi.getTopAnime(amt)
        if(MALResponse != null)
        {
            aniNames = MALResponse.map { (key, value) ->
                key to value.getOrDefault("title", "(Missing title)")
            }.toMap()
            aniCovers = MALResponse.map { (key, value) ->
                key to value.getOrDefault("cover", "https://banner2.cleanpng.com/20180402/tww/kisspng-emoji-iphone-text-messaging-sms-no-symbol-no-5ac1fb2a90a786.7966473015226621865925.jpg")
            }.toMap()
            return true
        }
        else
        {
            // Failed
            return false
        }
    }

 */

    fun getAniQuestions(amt: Int): ArrayList<AniQuestion>
    {
        // Create a list of questions
        var localOpPool:Map<Int, List<Int>>? = null
        if(themeSrc == ThemeSrc.Local)
        {
            // Using local raw files
            localOpPool = getLocalOps()!!.toMutableMap().filterKeys(aniNames::containsKey)
        }
        val questionList = ArrayList<AniQuestion>()
        for (i in 1..amt)
        {
            when (Random.nextInt(0, 2))
            {
                0 ->
                {
                    // Generating theme question
                    if(themeSrc == ThemeSrc.Local && localOpPool != null && localOpPool.isNotEmpty())
                    {
                        // Get themes from local raw files
                        val randomID = Random.nextInt(localOpPool.size)
                        questionList.add(AniQuestion(localOpPool.entries.elementAt(randomID).key, QType.Theme, localOpPool.entries.elementAt(randomID).value.random(), null))
                    }
                    else
                    {
                        // Theme will be streamed from API
                        questionList.add(AniQuestion(aniNames.keys.random(), QType.Theme, null, null))
                    }
                }
                1 ->
                {
                    // Generating cover question
                    val randomID = Random.nextInt(aniCovers.size)
                    questionList.add(AniQuestion(aniCovers.entries.elementAt(randomID).key, QType.Cover, null, aniCovers.entries.elementAt(randomID).value))
                }
            }
        }

        return questionList
    }

}
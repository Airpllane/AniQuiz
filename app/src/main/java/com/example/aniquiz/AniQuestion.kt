package com.example.aniquiz

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AniQuestion
(
    // Question data
    val aniID: Int,
    val type: QType,
    val rawID: Int?,
    var link: String?
) : Parcelable
package com.example.vitalio_cis.model


import com.google.gson.annotations.SerializedName

// ----------------------------------------------------
// MAIN RESPONSE MODEL
// ----------------------------------------------------

data class MediaResponse(

    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("responseValue")
    val responseValue: List<MediaItem>
)

// ----------------------------------------------------
// MEDIA ITEM MODEL
// ----------------------------------------------------

data class MediaItem(

    @SerializedName("id")
    val id: Int,

    @SerializedName("pmId")
    val pmId: Int,

    @SerializedName("url")
    val url: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("fileType")
    val fileType: String,

    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("dateTime")
    val dateTime: String,

    @SerializedName("createdDate")
    val createdDate: String,

    @SerializedName("status")
    val status: Boolean,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("clientID")
    val clientID: String?,

    @SerializedName("subcategory")
    val subcategory: String,

    @SerializedName("remark")
    val remark: String,

    @SerializedName("uhid")
    val uhid: String
)

// ----------------------------------------------------
// CATEGORY MODEL
// ----------------------------------------------------

data class ReportCategory(

    val label: String,

    val count: Int
)
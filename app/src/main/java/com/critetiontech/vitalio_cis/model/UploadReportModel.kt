package com.critetiontech.vitalio_cis.model

import com.google.gson.annotations.SerializedName

// ----------------------------------------------------
// MAIN RESPONSE MODEL
// ----------------------------------------------------

data class MediaResponse(

    @SerializedName("status")
    val status: Int? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("responseValue")
    val responseValue: List<MediaItem>? = emptyList()
)

// ----------------------------------------------------
// MEDIA ITEM MODEL
// ----------------------------------------------------

data class MediaItem(

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("pmId")
    val pmId: Int? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("fileType")
    val fileType: String? = null,

    @SerializedName("fileName")
    val fileName: String? = null,

    @SerializedName("dateTime")
    val dateTime: String? = null,

    @SerializedName("createdDate")
    val createdDate: String? = null,

    @SerializedName("status")
    val status: Boolean? = null,

    @SerializedName("userId")
    val userId: Int? = null,

    @SerializedName("clientID")
    val clientID: String? = null,

    @SerializedName("subcategory")
    val subcategory: String? = null,

    @SerializedName("remark")
    val remark: String? = null,

    @SerializedName("uhid")
    val uhid: String? = null
)

// ----------------------------------------------------
// CATEGORY MODEL
// ----------------------------------------------------

data class ReportCategory(

    val label: String,

    val count: Int
)
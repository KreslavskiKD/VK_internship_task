package com.example.vk_video_loader.models

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

data class VKVideo (
    val upload_url: String = "",
    val video_id: Int = 0,
    val title: String = "",
    val description: String = "",
    val owner_id: Int = 0) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(upload_url)
        dest.writeInt(video_id)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeInt(owner_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VKVideo> {
        override fun createFromParcel(source: Parcel): VKVideo {
            return VKVideo(source)
        }

        override fun newArray(size: Int): Array<VKVideo?> {
            return arrayOfNulls(size)
        }

        fun parse(json: JSONObject)
                = VKVideo(upload_url = json.optString("upload_url", ""),
                video_id = json.optInt("video_id", 0),
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                owner_id = json.optInt("owner_id", 0))
    }
}
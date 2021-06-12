package com.example.vk_video_loader

import com.vk.api.sdk.VKApiManager
import com.vk.api.sdk.VKApiResponseParser
import com.vk.api.sdk.VKMethodCall
import com.vk.api.sdk.exceptions.VKApiIllegalResponseException
import com.vk.api.sdk.internal.ApiCommand
import com.vk.api.sdk.requests.VKRequest
import org.json.JSONException
import org.json.JSONObject

class VKVideoSaveRequest(private val name: String? = null,
                         private val description: String? = null,
//                         private val is_private: Boolean = false,
                         private val wallpost: Boolean = false,   //// these features can be done later
//                         private val link: String? = null,
//                         private val group_id: Int = 0,
//                         private val album_id: Int = 0,
//                         private val privacy_view: String? = null,
//                         private val privacy_comment: String? = null,
//                         private val no_comments: Boolean = false,
                         private val repeat: Boolean = false,
                         private val compression: Boolean = false) : ApiCommand<String>()  {
    override fun onExecute(manager: VKApiManager): String {
        val callBuilder = VKMethodCall.Builder()
            .method("video.save")
//            .args("is_private", if (is_private) 1 else 0)
            .args("wallpost", if (wallpost) 1 else 0)
//            .args("no_comments", if (no_comments) 1 else 0)
            .args("repeat", if (repeat) 1 else 0)
            .args("compression", if (compression) 1 else 0)
            .version(manager.config.version)

        name?.let {
            callBuilder.args("name", it)
        }

        description?.let {
            callBuilder.args("description", it)
        }

        return manager.execute(callBuilder.build(), ResponseApiParser())
    }

    private class ResponseApiParser : VKApiResponseParser<String> {
        override fun parse(response: String): String {
            try {
                return JSONObject(response).getJSONObject("response").getString("upload_url")
            } catch (ex: JSONException) {
                throw VKApiIllegalResponseException(ex)
            }
        }
    }


}
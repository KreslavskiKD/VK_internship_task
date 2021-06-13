package com.example.vk_video_loader.models

import com.example.vk_video_loader.VideoFragment

data class ProgramState(
    var upload_mode: Boolean,
    var last_load_state: Int,
    var how_much_loaded: Long,
    var video_name: String,
    var video_description: String,
    var potw: Boolean,
    var repeat: Boolean,
    var compression: Boolean,
    var videos: MutableList<VideoFragment>)

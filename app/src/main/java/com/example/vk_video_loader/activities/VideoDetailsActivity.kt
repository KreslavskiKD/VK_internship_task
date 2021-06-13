package com.example.vk_video_loader.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.vk_video_loader.*
import com.example.vk_video_loader.models.ProgramState
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import okhttp3.*
import okio.*
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.*

class VideoDetailsActivity : AppCompatActivity(), CountingFileRequestBody.ProgressListener {

    lateinit var client: OkHttpClient

    lateinit var play_btn : FloatingActionButton
    lateinit var progress_bar : ProgressBar
    lateinit var statusTextView: TextView
    lateinit var cancelBtn: Button
    var transferredYetData: Long = 0
    var totalSize: Long = 0
    var listOfVideos: MutableList<VideoFragment> = mutableListOf()

    lateinit var source_file_path : String
    lateinit var m_call : Call

    var program_mode = false

    var video_name : String = ""
    var video_compression : Boolean = false
    var video_description : String = ""
    var video_repeat : Boolean = false
    var post_on_the_wall : Boolean = false
    var video_status: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_details)

        client = OkHttpClient.Builder()
            //               .socketFactory(RestrictedSocketFactory(16*1024))
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val intent = intent
        video_name = intent.getStringExtra("video_name")!!
        video_compression = intent.getBooleanExtra("video_compression", false)
        video_description = intent.getStringExtra("video_description")!!
        video_repeat = intent.getBooleanExtra("video_repeat", false)
        video_status = intent.getIntExtra("video_status", 1)
        source_file_path = intent.getStringExtra("source_file_path")!!
        post_on_the_wall = intent.getBooleanExtra("video_potw", false)
        program_mode = intent.getBooleanExtra("uploading_mode", false)


        val video_name_text_field = findViewById<TextView>(R.id.activity_video_name)
        val video_description_field = findViewById<TextView>(R.id.activity_video_description)
        val video_potw_field = findViewById<TextView>(R.id.activity_video_potw)
        val video_repeat_field = findViewById<TextView>(R.id.activity_video_repeat)
        val video_compression_field = findViewById<TextView>(R.id.activity_video_compress)
        statusTextView = findViewById(R.id.activity_video_status)
        cancelBtn = findViewById(R.id.cancel_btn)

        video_name_text_field.text = video_name
        video_description_field.text = video_description
        video_potw_field.text = post_on_the_wall.toString()
        video_compression_field.text = video_compression.toString()
        video_repeat_field.text = video_repeat.toString()

        play_btn = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        progress_bar = findViewById(R.id.progressBar3)
        progress_bar.setProgress(0, true)
        play_btn.hide()


        VK.execute(VKVideoSaveRequest(video_name, video_description, post_on_the_wall, video_repeat, video_compression), object: VKApiCallback<String> {
            override fun success(result: String) {
                video_status = LOADING
                play_btn.show()
                statusTextView.text = "Loading"

                var sourceFile = File(source_file_path)
                totalSize = sourceFile.length()
                Thread {
                    val mimeType = getMimeType(sourceFile);
                    if (mimeType == null) {
                        Log.e("file error", "Not able to get mime type")
                        return@Thread
                    }

                    try {
                        val requestBody: RequestBody =
                            MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart(
                                    "video",
                                    sourceFile.name,
                                    CountingFileRequestBody(sourceFile, "video/*", this@VideoDetailsActivity, 0L))
                                .build()

                        val request: Request = Request.Builder().url(result).post(requestBody).build()

                        m_call = client.newCall(request)
                        val response: Response = m_call.execute()

                        if (response.isSuccessful) {
                            Log.d("File upload","success")
                            this@VideoDetailsActivity.runOnUiThread {
                                Toast.makeText(
                                    this@VideoDetailsActivity,
                                    "Upload ended",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e("File upload", "failed")

                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        Log.e("File upload", "failed")
                        this@VideoDetailsActivity.runOnUiThread {
                            Toast.makeText(
                                this@VideoDetailsActivity,
                                "Upload failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                }.start()

            }

            override fun fail(error: Exception) {
                Log.e("uploading", error.toString())
            }
        })
    }

    fun getMimeType(file: File): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    override fun transferred(num: Long) {
        this@VideoDetailsActivity.runOnUiThread {
            transferredYetData = num
            var percents = ((num.toFloat() / totalSize.toFloat()) * 100).toInt()
            progress_bar.progress = percents
            if (percents >= 100) {
                progress_bar.visibility = View.GONE
                play_btn.hide()
                cancelBtn.visibility = View.GONE
                statusTextView.text = "Loaded"
                video_status = LOADED
            }
        }
    }

    fun onCancelButton(view: View) {
        if (!m_call.isCanceled()) {
            m_call.cancel()
            video_status = CANCELLED
            progress_bar.visibility = View.GONE
            play_btn.hide()
            statusTextView.text = "Cancelled"
            transferredYetData = 0
        }
    }

    override fun onPause() {
        super.onPause()
        if (program_mode && transferredYetData != totalSize) {
            video_status = PAUSED
            m_call.cancel()
            var gson = Gson()
            var file = File(this.cacheDir.toString() + "data.txt")
            var programState = ProgramState(program_mode,
                1, transferredYetData, video_name, video_description, post_on_the_wall, video_repeat, video_compression, listOfVideos)
            file.writeText(gson.toJson(programState))
        }
    }

    override fun onResume() {
        super.onResume()
        var gson = Gson()
        val bufferedReader : BufferedReader
        var last_load_state = 0

        var temp_dir = File(this.cacheDir.toString() + "data.txt")
        if (temp_dir.exists()) {
            bufferedReader = File(this.cacheDir.toString() + "data.txt").bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            var programState = gson.fromJson(inputString, ProgramState::class.java)
            program_mode = programState.upload_mode
            last_load_state = programState.last_load_state
            transferredYetData = programState.how_much_loaded
            listOfVideos = programState.videos


            val video_name = programState.video_name
            val video_compression = programState.compression
            val video_description = programState.video_description
            val video_repeat = programState.repeat
            val post_on_the_wall = programState.potw

            if (program_mode && last_load_state == 1) {

                VK.execute(
                    VKVideoSaveRequest(
                        video_name,
                        video_description,
                        post_on_the_wall,
                        video_repeat,
                        video_compression
                    ), object : VKApiCallback<String> {
                        override fun success(result: String) {
                            video_status = LOADING
                            play_btn.show()
                            statusTextView.text = "Loading"

                            var sourceFile = File(source_file_path)
                            totalSize = sourceFile.length()
                            Thread {
                                val mimeType = getMimeType(sourceFile);
                                if (mimeType == null) {
                                    Log.e("file error", "Not able to get mime type")
                                    return@Thread
                                }

                                try {
                                    val requestBody: RequestBody =
                                        MultipartBody.Builder()
                                            .setType(MultipartBody.FORM)
                                            .addFormDataPart(
                                                "video",
                                                sourceFile.name,
                                                CountingFileRequestBody(
                                                    sourceFile,
                                                    "video/*",
                                                    this@VideoDetailsActivity,
                                                    transferredYetData
                                                )
                                            )
                                            .build()

                                    val request: Request =
                                        Request.Builder().url(result).post(requestBody).build()

                                    m_call = client.newCall(request)
                                    val response: Response = m_call.execute()

                                    if (response.isSuccessful) {
                                        Log.d("File upload", "success")
                                        this@VideoDetailsActivity.runOnUiThread {
                                            Toast.makeText(
                                                this@VideoDetailsActivity,
                                                "Upload ended",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        Log.e("File upload", "failed")

                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    Log.e("File upload", "failed")
                                    this@VideoDetailsActivity.runOnUiThread {
                                        Toast.makeText(
                                            this@VideoDetailsActivity,
                                            "Upload failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                            }.start()

                        }

                        override fun fail(error: Exception) {
                            Log.e("uploading", error.toString())
                        }
                    })
            }
        }
    }

    fun onPauseBtn(view: View) {
        if (video_status == PAUSED) {
            play_btn.setImageResource(android.R.drawable.ic_media_pause)
            video_status = LOADING
            Log.d("pause_btn", "Clicked to continue")
//            var gson = Gson()
//            val bufferedReader : BufferedReader
 //           var last_load_state = 0

 //           var temp_dir = File(this.cacheDir.toString() + "data.txt")
 //           if (temp_dir.exists()) {
 //               bufferedReader = File(this.cacheDir.toString() + "data.txt").bufferedReader()
  //              val inputString = bufferedReader.use { it.readText() }
  //              var programState = gson.fromJson(inputString, ProgramState::class.java)
  //              program_mode = programState.upload_mode
  //              last_load_state = programState.last_load_state
  //              transferredYetData = programState.how_much_loaded
  //              listOfVideos = programState.videos


  //              val video_name = programState.video_name
 //               val video_compression = programState.compression
  //              val video_description = programState.video_description
 //               val video_repeat = programState.repeat
 //               val post_on_the_wall = programState.potw

  //              if (program_mode && last_load_state == 1) {

                    VK.execute(
                        VKVideoSaveRequest(
                            video_name,
                            video_description,
                            post_on_the_wall,
                            video_repeat,
                            video_compression
                        ), object : VKApiCallback<String> {
                            override fun success(result: String) {
                                video_status = LOADING
                                play_btn.show()
                                statusTextView.text = "Loading"

                                var sourceFile = File(source_file_path)
                                totalSize = sourceFile.length()
                                Thread {
                                    val mimeType = getMimeType(sourceFile);
                                    if (mimeType == null) {
                                        Log.e("file error", "Not able to get mime type")
                                        return@Thread
                                    }

                                    try {
                                        val requestBody: RequestBody =
                                            MultipartBody.Builder()
                                                .setType(MultipartBody.FORM)
                                                .addFormDataPart(
                                                    "video",
                                                    sourceFile.name,
                                                    CountingFileRequestBody(
                                                        sourceFile,
                                                        "video/*",
                                                        this@VideoDetailsActivity,
                                                        transferredYetData
                                                    )
                                                )
                                                .build()

                                        val request: Request =
                                            Request.Builder().url(result).post(requestBody).build()

                                        m_call = client.newCall(request)
                                        val response: Response = m_call.execute()

                                        if (response.isSuccessful) {
                                            Log.d("File upload", "success")
                                            this@VideoDetailsActivity.runOnUiThread {
                                                Toast.makeText(
                                                    this@VideoDetailsActivity,
                                                    "Upload ended",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            Log.e("File upload", "failed")

                                        }
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                        Log.e("File upload", "failed")
                                        this@VideoDetailsActivity.runOnUiThread {
                                            Toast.makeText(
                                                this@VideoDetailsActivity,
                                                "Upload failed",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                }.start()

                            }

                            override fun fail(error: Exception) {
                                Log.e("uploading", error.toString())
                            }
                        })
       //         }

                return
            }

            if (video_status == LOADING) {
                play_btn.setImageResource(android.R.drawable.ic_media_play)
                video_status = PAUSED
                Log.d("pause_btn", "Clicked to pause")


                m_call.cancel()
                var gson = Gson()
                var file = File(this.cacheDir.toString() + "data.txt")
                var programState = ProgramState(program_mode,
                    1, transferredYetData, video_name, video_description, post_on_the_wall, video_repeat, video_compression, listOfVideos)
                file.writeText(gson.toJson(programState))

                return
            }

        }


    }
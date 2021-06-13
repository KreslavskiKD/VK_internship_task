package com.example.vk_video_loader.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vk_video_loader.*
import com.example.vk_video_loader.models.ProgramState
import com.example.vk_video_loader.utilities.URIPathHelper
import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKTokenExpiredHandler
import java.io.BufferedReader
import java.io.File
import java.lang.Exception

const val LOADING: Int = 1
const val LOADED: Int = 2
const val PAUSED: Int = 3
const val CANCELLED: Int = -1


class MainActivity : AppCompatActivity(), VideoDialogReturnInterface {
    var program_mode: Boolean = false
    lateinit var video_name : String
    lateinit var video_description : String
    var post_on_the_wall : Boolean = false
    var video_repeat : Boolean = false
    var video_compression : Boolean = false
    var video_status: Int = 0
    var hml = 0L

    var listOfVideos: MutableList<VideoFragment> = mutableListOf()
    var adapter: VideoFragmentAdapter = VideoFragmentAdapter(this, listOfVideos)
    lateinit var layoutManager: LinearLayoutManager
    lateinit var recyclerView : RecyclerView
    lateinit var checkBox: CheckBox
    var REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        var programState = ProgramState(program_mode,
//            video_status, 0L, video_name, video_description, post_on_the_wall, video_repeat, video_compression, listOfVideos)
        var gson = Gson()
        val bufferedReader : BufferedReader
        //// todo add permission asking for WRITE_EXTERNAL_STORAGE

        var temp_file = File(this.cacheDir.toString() + "data.txt")
        if (temp_file.exists()) {
            bufferedReader = File(this.cacheDir.toString() + "data.txt").bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            var programState = gson.fromJson(inputString, ProgramState::class.java)
            if (programState != null) {
                program_mode = programState.upload_mode
                video_status = programState.last_load_state
                listOfVideos = programState.videos
            }
        } else {
            var file = File(this.cacheDir.toString() + "data.txt")
            file.createNewFile()
//            file.writeText(gson.toJson(programState))
        }

        checkBox = findViewById(R.id.check_type_box)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        adapter = VideoFragmentAdapter(baseContext, listOfVideos)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        VK.addTokenExpiredHandler(tokenTracker)

        val logoutBtn = findViewById<Button>(R.id.logout_btn)
        logoutBtn.setOnClickListener {
            VK.logout()
            VkAuthorize.startFrom(this)
            finish()
        }

        val uploadBtn : Button = findViewById(R.id.upload_button)
        program_mode = checkBox.isChecked
        uploadBtn.setOnClickListener {

            Log.d("uploadBtn", "Entered onClick method")

            val videoDialog = VideoDialog(this)
            var manager: FragmentManager = supportFragmentManager
            videoDialog.show(manager, "videoDialog")

            Log.d("uploadBtn", "videoDialog should've started")
        }
    }

    override fun onDialogPositiveClick(
            name: String,
            description: String,
            potw: Boolean,
            repeat: Boolean,
            compression: Boolean
    ) {
        setVideoName(name)
        setVideoDescription(description)
        setPOTW(potw)
        setRepeat(repeat)
        setVideoCompression(compression)


        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_PICK
        if (!isPermissionsAllowed()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this as Activity,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    )
            ) {
                AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("Permission is denied, Please allow permissions from App Settings.")
                        .setPositiveButton("App Settings",
                                DialogInterface.OnClickListener { dialogInterface, i ->
                                    // send to app settings if permission is denied permanently
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    val uri = Uri.fromParts("package", getPackageName(), null)
                                    intent.data = uri
                                    startActivity(intent)
                                })
                        .setNegativeButton("Cancel",null)
                        .show()
            } else {
                ActivityCompat.requestPermissions(
                        this as Activity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_CODE
                )
            }
        } else {
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CODE)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            if (data?.data != null) {
                val uriPathHelper = URIPathHelper()
                val intent = Intent(this, VideoDetailsActivity::class.java)
                intent.putExtra("video_name", video_name)
                intent.putExtra("video_description", video_description)
                intent.putExtra("video_potw", post_on_the_wall)
                intent.putExtra("video_repeat", video_repeat)
                intent.putExtra("video_compress", video_compression)
                intent.putExtra("status", video_status)
                intent.putExtra("source_file_path", uriPathHelper.getPath(this, data.data!!)!!)
                intent.putExtra("uploading_mode", program_mode)

                listOfVideos.add(VideoFragment(video_name, video_description, post_on_the_wall, video_repeat, video_compression, video_status))
                adapter = VideoFragmentAdapter(baseContext, listOfVideos)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()

                this.startActivity(intent)
            }
        }
    }


    private val tokenTracker = object: VKTokenExpiredHandler {
        override fun onTokenExpired() {
            // token expired
        }
    }

    override fun onDialogNegativeClick() {

    }

    fun setVideoName(name: String) { video_name = name }
    fun setVideoDescription(description: String) { video_description = description }
    fun setPOTW(post: Boolean) { post_on_the_wall = post}  /// next 3 lines can be upgraded to 1.5.0 Kotlin standard if allowed
    fun setRepeat(repeat: Boolean) { video_repeat = repeat }
    fun setVideoCompression(compression: Boolean) { video_compression = compression}


    fun isPermissionsAllowed() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED


    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<String>,grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent()
                    intent.type = "video/*"
                    intent.action = Intent.ACTION_PICK
                    startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CODE)
                } else {
                    // permission is denied, you can ask for permission again, if you want
                    //  askForPermissions()
                }
                return
            }
        }
    }


    companion object {
        private const val TAG = "UserActivity"

        fun startFrom(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        program_mode = checkBox.isChecked
        var gson = Gson()
        var file = File(this.cacheDir.toString() + "data.txt")
        var programState = ProgramState(program_mode, video_status, hml, video_name, video_description, post_on_the_wall, video_repeat, video_compression, listOfVideos)
        file.writeText(gson.toJson(programState))
    }

    override fun onResume() {
        super.onResume()
        var gson = Gson()
        val bufferedReader : BufferedReader

        var temp_dir = File(this.cacheDir.toString() + "data.txt")
        if (temp_dir.exists()) {
            bufferedReader = File(this.cacheDir.toString() + "data.txt").bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            var programState = gson.fromJson(inputString, ProgramState::class.java)
            if (programState != null) {
                program_mode = programState.upload_mode
                video_status = programState.last_load_state
                listOfVideos = programState.videos
                hml = programState.how_much_loaded
            }
        }
    }
}
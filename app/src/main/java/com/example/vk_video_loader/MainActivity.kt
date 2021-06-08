package com.example.vk_video_loader

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.VKTokenExpiredHandler
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import com.vk.api.sdk.ui.VKConfirmationActivity
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), VideoDialogReturnInterface {
    lateinit var video_name : String
    lateinit var video_description : String
    var post_on_the_wall : Boolean = false
    var video_repeat : Boolean = false
    var video_compression : Boolean = false

    var listOfVideos: MutableList<VideoFragment> = mutableListOf()
    var adapter: VideoFragmentAdapter = VideoFragmentAdapter(this, listOfVideos)
    lateinit var layoutManager: LinearLayoutManager
    lateinit var recyclerView : RecyclerView
    var REQUEST_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        VK.addTokenExpiredHandler(tokenTracker)

        val logoutBtn = findViewById<Button>(R.id.logout_btn)
        logoutBtn.setOnClickListener {
            VK.logout()
            VkAuthorize.startFrom(this)
            finish()
        }

        val uploadBtn : Button = findViewById(R.id.upload_button)
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

        listOfVideos.add(VideoFragment(video_name, video_description, post_on_the_wall, video_repeat, video_compression))

        adapter = VideoFragmentAdapter(baseContext, listOfVideos)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()


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
                val videoFullPath = uriPathHelper.getPath(this, data.data!!)!!
                VK.execute(VKVideoSaveRequest(video_name, video_description, post_on_the_wall, video_repeat, video_compression), object: VKApiCallback<String> {
                    override fun success(result: String) {
                        UploadUtility(this@MainActivity).uploadFile(videoFullPath, result)

                    }

                    override fun fail(error: Exception) {
                        Log.e(TAG, error.toString())
                    }
                })
            }
        }
    }


    private val tokenTracker = object: VKTokenExpiredHandler {
        override fun onTokenExpired() {
            // token expired
        }
    }


    override fun onDialogNegativeClick() {
        TODO("Not yet sure what to do here")
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
}
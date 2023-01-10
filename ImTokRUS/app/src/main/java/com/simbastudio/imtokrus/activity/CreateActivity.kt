package com.simbastudio.imtokrus.activity

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.simbastudio.imtokrus.databinding.ActivityCreateBinding
import java.io.ByteArrayOutputStream
import java.util.*


@Suppress("DEPRECATION")
class CreateActivity : AppCompatActivity() {

    lateinit var binding: ActivityCreateBinding

    private var uploadedUri: Uri? = null

    private var isVideo: Boolean? = false

    private lateinit var mAuth: FirebaseAuth

    private var pDialog: ProgressDialog? = null

    private var myUrl = ""

    private lateinit var firebaseUser: FirebaseUser

    private var postIv: ImageView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        binding.createPostBtn.setOnClickListener {   createFirebasePost() }

        binding.uploadIv.setOnClickListener {
            chooseImage()
        }
    }

    private fun chooseImage() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            )
            intent.type = "*/*"
            startActivityForResult(intent, SignUp.PICK_IMAGE_REQUEST_CODE)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                SignUp.READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SignUp.PICK_IMAGE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }
            val uri = data?.data
            if (uri.toString().indexOf("video") != -1) {
                binding.uploadContainer.isVisible = false
                binding.postIv.isVisible = false
                binding.videoView.isVisible = true
                binding.videoView.setVideoURI(uri)
                binding.videoView.requestFocus()
                binding.videoView.start()
                uploadedUri = uri
                isVideo = true
            } else if (uri != null) {
                binding.uploadContainer.isVisible = false
                binding.videoView.isVisible = false
                binding.postIv.isVisible = true;
                val imageBitmap = uriToBitmap(uri)
                Glide.with(this)
                    .load(imageBitmap)
                    .centerCrop()
                    .into(binding.postIv);
                binding.uploadContainer.isVisible = false
                uploadedUri = uri
                isVideo = false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SignUp.READ_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // pick image after request permission success
                    chooseImage()
                }
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    private fun goToMainActivity() {
        intent = Intent(this@CreateActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun createFirebasePost(){
        mAuth = FirebaseAuth.getInstance()
        if(mAuth.currentUser != null){

            val ref = FirebaseDatabase.getInstance().reference.child("Posts")
            val postId = ref.push().key

            val storage = Firebase.storage
            val storageRef = storage.reference
            val uuid = UUID.randomUUID()
            val refLink = if (isVideo == true) "Post Pictures/" + uuid + ".mp4" else "Post Pictures/" + uuid + ".jpeg"
            val postRef = storageRef.child(refLink)

            val uploadTask: StorageTask<*>
            uploadTask = if(isVideo == true) postRef.putFile(uploadedUri!!) else postRef.putBytes(getUploadedImage())

            uploadTask.continueWithTask<Uri?>(Continuation <UploadTask.TaskSnapshot, Task<Uri>>{task ->
                if(!task.isSuccessful){
                    task.exception?.let {
                        throw it
                        pDialog!!.dismiss()
                    }
                }
                return@Continuation postRef.downloadUrl
            }).addOnCompleteListener(OnCompleteListener<Uri>{task ->
                if(task.isSuccessful){
                    val downloadUrl = task.result
                    myUrl = downloadUrl.toString()

                    val postMap = HashMap<String, Any>()
                    postMap["id"] = postId!!
                    postMap["description"] = binding.descriptionPost.text.toString().toLowerCase()
                    postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
                    postMap["postcategory"] = if(isVideo == true) 2 else 1
                    postMap["postcontent"] = myUrl

                    ref.child(postId).updateChildren(postMap)

                    Toast.makeText(this@CreateActivity, "Ваш пост успешно опубликован!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                }
            })
        }else {
            Toast.makeText(this@CreateActivity, "Ошибка!", Toast.LENGTH_SHORT).show()
            pDialog!!.dismiss()
        }
    }

    private fun getUploadedImage(): ByteArray {
        postIv?.isDrawingCacheEnabled = true
        postIv?.buildDrawingCache()
        val bitmap = (postIv?.drawable as? BitmapDrawable)?.bitmap
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }
}
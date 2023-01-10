package com.simbastudio.imtokrus.activity


import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.databinding.ActivityAccountSettingsBinding
import com.simbastudio.imtokrus.model.User
import com.squareup.picasso.Picasso

class AccountSettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivityAccountSettingsBinding

    lateinit var mAuth : FirebaseAuth

    private lateinit var firebaseUser: FirebaseUser

    private var checker = ""

    private var imageUri: Uri? = null

    private var myUrl = ""

    private lateinit var progressDialog: ProgressDialog

    private var storageProfilePickRef: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        storageProfilePickRef = FirebaseStorage.getInstance().reference.child("Profile Pictures")

        mAuth = FirebaseAuth.getInstance()

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        binding.logoutBtnProfile.setOnClickListener {
            mAuth.signOut()

            val intent = Intent(this, LogIn::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        binding.profileImage.setOnClickListener {
            checker = "clicked"

            showImageAttachMenu()
        }

        binding.saveSettingsProfileBtn.setOnClickListener {
            if(checker == "clicked")
            {
                uploadImage()
            }
            else
            {
                updateUserInfoOnly()
            }
        }

        userInfo()

        binding.btnVerify.setOnClickListener {
            emailVerification()
        }

        binding.btnChangePass.setOnClickListener {
            changePass()
        }

        binding.btnChangeEmail.setOnClickListener {
            changeEmail()
        }
    }

    private fun showImageAttachMenu(){

        ///////Показать меню выбора фото из галереи или с камеры////
        val popupMenu = PopupMenu(this, binding.profileImage)
        //popupMenu.menu.add(Menu.NONE, 0, 0, "С камеры")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Из галереи")
        popupMenu.show()

        ///////handle popup menu item click
        popupMenu.setOnMenuItemClickListener {item ->
            //get id of clicked item
            val id = item.itemId

            if(id == 0){
                //Camera clicked
                pickImageCamera()
            }
            else if(id == 1)
            {
                //Gallery clicked
                pickImageGallery()
            }

            true
        }

    }

    private fun pickImageCamera(){
        //intent to pick image from camera
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGallery(){
        //intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    //used to haandle result of camera intent (new way in replacement of startactivityforresults)
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {result ->
            //get uri of image
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                //imageUri = data!!.data no need we already have image in imageUri in camera case

                //set to image view
                binding.profileImage.setImageURI(imageUri)
            }
            else{
                //Cancelled
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    )

    //used to haandle result of gallery intent (new way in replacement of startactivityforresults)
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback {result ->
            //get uri of image
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data

                //set to image view
                binding.profileImage.setImageURI(imageUri)
            }
            else{
                //Cancelled
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    private fun uploadImage(){

        if(binding.fullNameProfileFrag.text.toString() == "")
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else if(binding.usernameProfileFrag.text.toString() == "")
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else if(binding.aboutAccountInfo.text.toString() == "")
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else if(imageUri == null)
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else
        {
            progressDialog.setMessage("Upload profile image")
            progressDialog.show()

            val fileRef = storageProfilePickRef!!.child(firebaseUser!!.uid + ".jpg")

            var uploadTask: StorageTask<*>
            uploadTask = fileRef.putFile(imageUri!!)

            uploadTask.continueWithTask<Uri?>(Continuation <UploadTask.TaskSnapshot, Task<Uri>>{ task ->
                if(!task.isSuccessful)
                {
                    task.exception?.let {
                        throw it
                        progressDialog.dismiss()
                    }
                }
                return@Continuation fileRef.downloadUrl
            }).addOnCompleteListener ( OnCompleteListener<Uri> {task ->
                if(task.isSuccessful)
                {
                    val downloadUrl = task.result
                    myUrl = downloadUrl.toString()

                    val ref = FirebaseDatabase.getInstance().reference.child("Users")

                    val userMap = HashMap<String, Any>()
                    userMap["fullname"] = binding.fullNameProfileFrag.text.toString().toLowerCase()
                    userMap["username"] = binding.usernameProfileFrag.text.toString().toLowerCase()
                    userMap["bio"] = binding.aboutAccountInfo.text.toString().toLowerCase()
                    userMap["image"] = myUrl

                    ref.child(firebaseUser.uid).updateChildren(userMap)

                    Toast.makeText(this, "Информация обновлена успешно!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    progressDialog.dismiss()
                }
                else
                {
                    progressDialog.dismiss()
                }
            } )
        }
    }

    private fun updateUserInfoOnly() {

        if(binding.fullNameProfileFrag.text.toString() == "")
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else if(binding.usernameProfileFrag.text.toString() == "")
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else if(binding.aboutAccountInfo.text.toString() == "")
        {
            Toast.makeText(this, "Поле не может быть пустым!", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val usersRef = FirebaseDatabase.getInstance().reference.child("Users")

            val userMap = HashMap<String, Any>()
            userMap["fullname"] = binding.fullNameProfileFrag.text.toString().toLowerCase()
            userMap["username"] = binding.usernameProfileFrag.text.toString().toLowerCase()
            userMap["bio"] = binding.aboutAccountInfo.text.toString().toLowerCase()

            usersRef.child(firebaseUser.uid).updateChildren(userMap)

            Toast.makeText(this, "Информация обновлена успешно!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }



    private fun changeEmail(){
        val intent = Intent(this, ChangeEmailActivity::class.java)
        startActivity(intent)
    }

    private fun emailVerification(){
        val user = mAuth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener {
            if(it.isSuccessful){
                Toast.makeText(this, "Было Отправлено Электронное письмо с Подтверждением", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePass(){
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        binding.cvCurrentPass.visibility = View.VISIBLE

        binding.btnCancel.setOnClickListener {
            binding.cvCurrentPass.visibility = View.GONE
        }

        binding.btnConfirm.setOnClickListener btnConfirm@{
            val pass = binding.edtCurrentPassword.text.toString()

            if(pass.isEmpty()){
                binding.edtCurrentPassword.error = "Пароль не должен быть пустым"
                binding.edtCurrentPassword.requestFocus()
                return@btnConfirm
            }

            user.let {
                val userCredential = EmailAuthProvider.getCredential(it?.email!!, pass)
                it.reauthenticate(userCredential).addOnCompleteListener{ task ->
                    when{
                        task.isSuccessful -> {
                            binding.cvCurrentPass.visibility = View.GONE
                            binding.cvUpdatePass.visibility = View.VISIBLE
                        }
                        task.exception is FirebaseAuthInvalidCredentialsException -> {
                            binding.edtCurrentPassword.error = "Пароль введён неверно"
                            binding.edtCurrentPassword.requestFocus()
                        }
                        else -> {
                            Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            binding.btnNewCancel.setOnClickListener {
                binding.cvCurrentPass.visibility = View.GONE
                binding.cvUpdatePass.visibility = View.GONE
            }

            binding.btnNewChange.setOnClickListener newChangePassword@{
                val newPass = binding.edtNewPass.text.toString()
                val passConfirm = binding.edtConfirmPas.text.toString()

                if(newPass.isEmpty()){
                    binding.edtCurrentPassword.error = "Пароль не должен быть пустым"
                    binding.edtCurrentPassword.requestFocus()
                    return@newChangePassword
                }

                if(passConfirm.isEmpty()){
                    binding.edtCurrentPassword.error = "Повторите новый пароль"
                    binding.edtCurrentPassword.requestFocus()
                    return@newChangePassword
                }

                if(newPass.length < 8) {
                    binding.edtCurrentPassword.error = "Пароли Не Совпадают"
                    binding.edtCurrentPassword.requestFocus()
                    return@newChangePassword
                }

                if(newPass != passConfirm){
                    binding.edtCurrentPassword.error = "Пароли Не Совпадают"
                    binding.edtCurrentPassword.requestFocus()
                    return@newChangePassword
                }

                user?.let {
                    user.updatePassword(newPass).addOnCompleteListener {
                        if(it.isSuccessful){
                            Toast.makeText(this, "Пароль успешно обновлён", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun userInfo(){
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser.uid)

        usersRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val user = snapshot.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.ic_account_circle_black).into(binding.profileImage)
                    binding.usernameProfileFrag.setText(user.getUsername())
                    binding.fullNameProfileFrag.setText(user.getFullname())
                    binding.aboutAccountInfo.setText(user.getBio())
                }
            }
            override fun onCancelled(error: DatabaseError)
            {

            }
        })
    }
}
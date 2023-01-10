package com.simbastudio.imtokrus.activity


import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.simbastudio.imtokrus.databinding.ActivitySignUpBinding

class SignUp() : AppCompatActivity() {


    lateinit var binding: ActivitySignUpBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        binding.tvToLogin.setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            createAccount()
        }

        binding.politicsConfidential.setOnClickListener {
            val intent2 = Intent(this, PoliticConfidential::class.java)
            startActivity(intent2)
        }

    }

    private fun createAccount(){
        val fullName = binding.edtFullName.text.toString()
        val userName = binding.edtUserName.text.toString()
        val email = binding.edtEmailRegister.text.toString()
        val password = binding.edtPasswordRegister.text.toString()

        when{
            TextUtils.isEmpty(fullName) -> Toast.makeText(this, "Данное поле не может быть пустым", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(userName) -> Toast.makeText(this, "Данное поле не может быть пустым", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(email) -> Toast.makeText(this, "Данное поле не может быть пустым", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(password) -> Toast.makeText(this, "Данное поле не может быть пустым", Toast.LENGTH_LONG).show()

            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Регистрация")
                progressDialog.setMessage("Происходит регистрация пожалуйста подождите")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener {  task ->
                        if(task.isSuccessful){
                        saveUserInfo(fullName, userName, email, progressDialog)
                    }else{
                        val message = task.exception!!.toString()
                        Toast.makeText(this, "Ошибка: $message", Toast.LENGTH_LONG).show()
                        mAuth.signOut()
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }



    private fun saveUserInfo(fullName: String, userName: String, email: String, progressDialog: ProgressDialog) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val usersRef : DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")

        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserId
        userMap["fullname"] = fullName.toLowerCase()
        userMap["username"] = userName.toLowerCase()
        userMap["email"] = email
        userMap["bio"] = "Всем привет!!"
        userMap["image"] = "https://firebasestorage.googleapis.com/v0/b/intokrus-a0a07.appspot.com/o/Default%20images%2Flogo.png?alt=media&token=47928fbd-7254-40a8-aba4-641969208b48"

        usersRef.child(currentUserId).setValue(userMap).addOnCompleteListener { task ->
            if(task.isSuccessful){
                progressDialog.dismiss()
                Toast.makeText(this, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show()


                    FirebaseDatabase.getInstance().reference
                        .child("Follow").child(currentUserId)
                        .child("Following").child(currentUserId)
                        .setValue(true)

                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }else{
                val message = task.exception!!.toString()
                Toast.makeText(this, "Ошибка: $message", Toast.LENGTH_LONG).show()
                mAuth.signOut()
                progressDialog.dismiss()
            }
        }
    }

    companion object {
        const val PICK_IMAGE_REQUEST_CODE = 1000
        const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1001
        const val EMPTY_STRING = ""
    }
}
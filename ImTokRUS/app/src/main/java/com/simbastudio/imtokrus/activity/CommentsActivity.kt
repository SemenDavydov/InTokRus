package com.simbastudio.imtokrus.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.adapter.CommentsAdapter
import com.simbastudio.imtokrus.databinding.ActivityCommentsBinding
import com.simbastudio.imtokrus.model.Comment
import com.simbastudio.imtokrus.model.User
import com.squareup.picasso.Picasso

class CommentsActivity : AppCompatActivity()
{

    lateinit var binding: ActivityCommentsBinding
    private var postId = ""
    private var publisherId = ""
    private var firebaseUser: FirebaseUser? = null
    private var commentsAdapter: CommentsAdapter? = null
    private var commentList: MutableList<Comment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        postId = intent.getStringExtra("postId").toString()
        publisherId = intent.getStringExtra("publisherId").toString()

        firebaseUser = FirebaseAuth.getInstance().currentUser

        var recyclerView : RecyclerView
        recyclerView = findViewById(R.id.recycler_view_comments)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true
        recyclerView.layoutManager = linearLayoutManager

        commentList = ArrayList()
        commentsAdapter = CommentsAdapter(this, commentList)
        recyclerView.adapter = commentsAdapter


        userInfo()
        readComments()
        getPostImage()

        binding.postComment.setOnClickListener {
            if(binding.addComment!!.text.toString() == "")
            {
                Toast.makeText(this, "Пожалуйста введите ваш комментарий", Toast.LENGTH_SHORT).show()
            }
            else
            {
                addComment()
            }
        }
    }

    private fun addComment() {
        val commentsRef = FirebaseDatabase.getInstance().reference.child("Comments").child(postId!!)

        val commentsMap = HashMap<String, Any>()
        commentsMap["comment"] = binding.addComment!!.text.toString()
        commentsMap["publisher"] = firebaseUser!!.uid

        commentsRef.push().setValue(commentsMap)

        addNotification()

        binding.addComment!!.text.clear()
    }

    private fun userInfo(){
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)

        usersRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val user = snapshot.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.ic_account_circle_black).into(binding.profileImageComment)
                }
            }
            override fun onCancelled(error: DatabaseError)
            {

            }
        })
    }

    private fun getPostImage(){
        val postRef = FirebaseDatabase.getInstance()
            .reference.child("Posts")
            .child(postId!!).child("postimage")

        postRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val image = snapshot.value.toString()

                    Picasso.get().load(image).placeholder(R.drawable.ic_account_circle_black).into(binding.postImageComment)
                }
            }
            override fun onCancelled(error: DatabaseError)
            {

            }
        })
    }

    private fun readComments(){
        val commentsRef = FirebaseDatabase.getInstance()
            .reference.child("Comments")
            .child(postId)

        commentsRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot)
            {
                if(snapshot.exists())
                {
                    commentList!!.clear()

                    for(p0 in snapshot.children)
                    {
                        val comment = p0.getValue(Comment::class.java)
                        commentList!!.add(comment!!)
                    }
                    commentsAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addNotification()
    {
        val notiRef = FirebaseDatabase.getInstance()
            .reference.child("Notifications")
            .child(publisherId)

        val notiMap = HashMap<String, Any>()
        notiMap["userId"] = firebaseUser!!.uid
        notiMap["text"] = "Прокоментировал: " + binding.addComment!!.text.toString()
        notiMap["postId"] = postId
        notiMap["isPost"] = true

        notiRef.push().setValue(notiMap)
    }
}
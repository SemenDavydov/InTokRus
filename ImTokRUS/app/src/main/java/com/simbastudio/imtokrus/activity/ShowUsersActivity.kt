package com.simbastudio.imtokrus.activity


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.simbastudio.imtokrus.R

import com.simbastudio.imtokrus.adapter.UserAdapter
import com.simbastudio.imtokrus.databinding.ActivityShowUsersBinding
import com.simbastudio.imtokrus.model.User

class ShowUsersActivity : AppCompatActivity()
{

    lateinit var binding: ActivityShowUsersBinding
    var id: String = ""
    var title: String = ""

    var userAdapter: UserAdapter? = null
    var userList: List<User>? = null
    var idList: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        id = intent.getStringExtra("id").toString()
        title = intent.getStringExtra("title").toString()

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.title = title
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        var recyclerView: RecyclerView
        recyclerView = binding.recyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = ArrayList()
        userAdapter = UserAdapter(this, userList as ArrayList<User>, false)
        recyclerView.adapter = userAdapter

        idList = ArrayList()

        when(title)
        {
            "likes" -> getLikes()
            "following" -> getFollowing()
            "followers" -> getFollowers()
            "views" -> getViews()
        }
    }

    private fun getViews() {

    }

    private fun getFollowers()
    {
        val followersRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(id)
            .child("Followers")


        followersRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot)
            {
                (idList as ArrayList<String>).clear()

                for(p0 in snapshot.children)
                {
                    (idList as ArrayList<String>).add(p0.key!!)
                }
                showUsers()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getFollowing()
    {
        val followersRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(id)
            .child("Following")


        followersRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot)
            {
                (idList as ArrayList<String>).clear()

                for(p0 in snapshot.children)
                {
                    (idList as ArrayList<String>).add(p0.key!!)
                }
                showUsers()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getLikes()
    {

        val LikesRef = FirebaseDatabase.getInstance().reference
            .child("Likes").child(id)

        LikesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    (idList as ArrayList<String>).clear()

                    for(p0 in snapshot.children)
                    {
                        (idList as ArrayList<String>).add(p0.key!!)
                    }
                    showUsers()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun showUsers()
    {
        val usersRef = FirebaseDatabase.getInstance().getReference().child("Users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (userList as ArrayList<User>).clear()

                for (snapshot in dataSnapshot.children){
                    val user = snapshot.getValue(User::class.java)

                    for(id in idList!!)
                    {
                        if(user!!.uid == id){
                            (userList as ArrayList<User>).add(user)
                        }
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
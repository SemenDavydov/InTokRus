package com.simbastudio.imtokrus.fragment



import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.simbastudio.imtokrus.activity.AccountSettingsActivity
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.activity.ShowUsersActivity
import com.simbastudio.imtokrus.adapter.MyImagesAdapter
import com.simbastudio.imtokrus.model.Post
import com.simbastudio.imtokrus.model.User
import com.squareup.picasso.Picasso
import java.util.Collections


class UserFragment : Fragment(){


    private lateinit var profileId: String
    private lateinit var firebaseUser: FirebaseUser
    lateinit var mAuth : FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    var postList: List<Post>? = null
    var myImagesAdapter: MyImagesAdapter? = null

    var myImagesAdapterSaved: MyImagesAdapter? = null
    var postListSaved: List<Post>? = null
    var mySavesImg: List<String>? = null


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        if(pref != null)
        {
            this.profileId = pref.getString("profileId", "none").toString()
        }

        ///////////////////////Если это профиль самого пользователя то текст кнопки не изменится////////
        if(profileId == firebaseUser.uid)
        {
            view?.findViewById<Button>(R.id.edit_account_settings_btn)?.text = "Редактировать профиль"
        }
        else if(profileId != firebaseUser.uid)
        {
            checkFollowAndFollowingButtonStatus()
        }

        //recyclerView for Upload Images
        var recyclerViewUploadImages: RecyclerView
        recyclerViewUploadImages = view.findViewById(R.id.recycler_view_upload_pic)
        recyclerViewUploadImages.setHasFixedSize(true)
        val linearLayoutManager: LinearLayoutManager = GridLayoutManager(context, 3)
        recyclerViewUploadImages.layoutManager = linearLayoutManager

        postList = ArrayList()
        myImagesAdapter = context?.let { MyImagesAdapter(it, postList as ArrayList<Post>) }
        recyclerViewUploadImages.adapter = myImagesAdapter


        //recyclerView for Saved Images
        var recyclerViewSavedImages: RecyclerView
        recyclerViewSavedImages = view.findViewById(R.id.recycler_view_saved_pic)
        recyclerViewSavedImages.setHasFixedSize(true)
        val linearLayoutManager2: LinearLayoutManager = GridLayoutManager(context, 3)
        recyclerViewSavedImages.layoutManager = linearLayoutManager2

        postListSaved = ArrayList()
        myImagesAdapterSaved = context?.let { MyImagesAdapter(it, postListSaved as ArrayList<Post>) }
        recyclerViewSavedImages.adapter = myImagesAdapterSaved
        ///////////////////////////////////////////////////////////////////////////////////////////////


        recyclerViewSavedImages.visibility = View.GONE
        recyclerViewUploadImages.visibility = View.VISIBLE


        var uploadedImagesBtn: ImageButton
        uploadedImagesBtn = view.findViewById(R.id.images_grid_view_btn)
        uploadedImagesBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.GONE
            recyclerViewUploadImages.visibility = View.VISIBLE
        }


        var savedImagesBtn: ImageButton
        savedImagesBtn = view.findViewById(R.id.images_save_btn)
        savedImagesBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.VISIBLE
            recyclerViewUploadImages.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.total_followers).setOnClickListener {
            val intent = Intent(context, ShowUsersActivity::class.java)
            intent.putExtra("id", profileId)
            intent.putExtra("title", "followers")
            startActivity(intent)
        }

        view.findViewById<TextView>(R.id.total_following).setOnClickListener {
            val intent = Intent(context, ShowUsersActivity::class.java)
            intent.putExtra("id", profileId)
            intent.putExtra("title", "following")
            startActivity(intent)
        }

        //////////////////////////При нажатии на кнопку редактировать профиль/////////////////////////
        view.findViewById<Button>(R.id.edit_account_settings_btn).setOnClickListener {
            val getButtonText = view.findViewById<Button>(R.id.edit_account_settings_btn).text.toString()

            when
            {
                getButtonText == "Редактировать профиль" -> startActivity(Intent(context, AccountSettingsActivity::class.java))

                getButtonText == "Подписаться" ->
                {
                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it.toString())
                            .child("Following").child(profileId)
                            .setValue(true)
                    }

                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(profileId)
                            .child("Followers").child(it.toString())
                            .setValue(true)
                    }

                    addNotification()
                }

                getButtonText == "Отписаться" ->
                {
                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it.toString())
                            .child("Following").child(profileId)
                            .removeValue()
                    }

                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(profileId)
                            .child("Followers").child(it.toString())
                            .removeValue()
                    }
                }
            }

        }
        ////////////////////////////////////////////////////////////////////////////////////////////

        getFollowers()
        getFollowings()
        userInfo()
        myPhotos()
        getTotalNumberOfPosts()
        mySaves()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        ////////////////////Проверяю верефицирован ли пользователь//////////////////////////////////
        if(user!!.isEmailVerified){
            view.findViewById<TextView>(R.id.message_not_verify).visibility = View.GONE
            view.findViewById<ImageView>(R.id.icon_verify).visibility = View.VISIBLE
            view.findViewById<ImageView>(R.id.icon_notVerify).visibility = View.GONE
        }else{
            view.findViewById<TextView>(R.id.message_not_verify).visibility = View.VISIBLE
            view.findViewById<ImageView>(R.id.icon_verify).visibility = View.GONE
            view.findViewById<ImageView>(R.id.icon_notVerify).visibility = View.VISIBLE
        }
        ////////////////////////////////////////////////////////////////////////////////////////////
    }

    override fun onStop() {
        super.onStop()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", firebaseUser.uid)
        pref?.apply()
    }

    override fun onPause() {
        super.onPause()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", firebaseUser.uid)
        pref?.apply()
    }

    override fun onDestroy() {
        super.onDestroy()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", firebaseUser.uid)
        pref?.apply()
    }

    /////////////Если пользователь подписался то в профиле на того на кого он подписался, будет изменён текст кнопки редактирования профиля////////////
    private fun checkFollowAndFollowingButtonStatus() {
        val followingRef = firebaseUser?.uid.let { it ->
            FirebaseDatabase.getInstance().reference
                .child("Follow").child(it.toString())
                .child("Following")
        }

        if(followingRef != null)
        {
            followingRef.addValueEventListener(object  : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.child(profileId).exists()){
                        view?.findViewById<Button>(R.id.edit_account_settings_btn)?.text = "Отписаться"
                    }
                    else{
                        view?.findViewById<Button>(R.id.edit_account_settings_btn)?.text = "Подписаться"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////Загружаем и отображаем количество подписчиков////////////////////////////////////////////////////////
    private fun getFollowers(){
        val followersRef = FirebaseDatabase.getInstance().reference
                .child("Follow").child(profileId)
                .child("Followers")


        followersRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    view?.findViewById<TextView>(R.id.total_followers)?.text = snapshot.childrenCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////Загружаем и отображаем количество наших подписок///////////////////////////////////////////////////////////
    private fun getFollowings(){
        val followersRef = FirebaseDatabase.getInstance().reference
                .child("Follow").child(profileId)
                .child("Following")


        followersRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    view?.findViewById<TextView>(R.id.total_following)?.text = snapshot.childrenCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun myPhotos(){
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postsRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot)
            {
                if(snapshot.exists())
                {
                    (postList as ArrayList<Post>).clear()

                    for(p0 in snapshot.children)
                    {
                        val post = p0.getValue(Post::class.java)!!
                        if(post.publisher.equals(profileId))
                        {
                            (postList as ArrayList<Post>).add(post)
                        }
                        Collections.reverse(postList)
                        myImagesAdapter!!.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    //////////////////////////////////////////////Загружаем и отображаем данные о пользователе/////////////////////////////////////////////////
    private fun userInfo(){
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(profileId)

        usersRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val user = snapshot.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.ic_account_circle_black).into(view?.findViewById(R.id.account_photo))

                    view?.findViewById<TextView>(R.id.username_profile)?.text = user!!.getUsername()
                    view?.findViewById<TextView>(R.id.fullname_profile_frag)?.text = user!!.getFullname()
                    view?.findViewById<TextView>(R.id.about_user)?.text = user!!.getBio()
                    view?.findViewById<TextView>(R.id.edt_email)?.text = user!!.getEmail()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun getTotalNumberOfPosts()
    {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    var postCounter = 0

                    for(snapShot in dataSnapshot.children)
                    {
                        val post = snapShot.getValue(Post::class.java)
                        if(post!!.publisher == profileId)
                        {
                            postCounter++
                        }
                    }
                    view?.findViewById<TextView>(R.id.total_posts)!!.text = " " + postCounter
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mySaves()
    {
        mySavesImg = ArrayList()

        val savedRef = FirebaseDatabase.getInstance()
            .reference
            .child("Saves").child(firebaseUser.uid)

        savedRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists())
                {
                    for(snapShot in dataSnapshot.children)
                    {
                        (mySavesImg as ArrayList<String>).add(snapShot.key!!)
                    }
                    readSavedImagesData()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun readSavedImagesData()
    {
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postsRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists())
                {
                    (postListSaved as ArrayList<Post>).clear()

                    for(snapshot in dataSnapshot.children)
                    {
                        val post = snapshot.getValue(Post::class.java)

                        for(key in mySavesImg!!)
                        {
                            if(post!!.id == key)
                            {
                                (postListSaved as ArrayList<Post>).add(post)
                            }
                        }
                    }
                    myImagesAdapterSaved!!.notifyDataSetChanged()
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
            .child(profileId)

        val notiMap = HashMap<String, Any>()
        notiMap["userId"] = firebaseUser!!.uid
        notiMap["text"] = "Подписался на Вас!"
        notiMap["postId"] = ""
        notiMap["isPost"] = false

        notiRef.push().setValue(notiMap)
    }
}

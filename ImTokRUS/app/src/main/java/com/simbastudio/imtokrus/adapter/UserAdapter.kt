package com.simbastudio.imtokrus.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.simbastudio.imtokrus.model.User
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.activity.MainActivity
import com.simbastudio.imtokrus.fragment.UserFragment
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter (private var mContext : Context, private var mUser : List<User>, private var isFragment : Boolean = false) : RecyclerView.Adapter<UserAdapter.ViewHolder>(){

    private var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.user_item_layout, parent, false)
        return UserAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mUser.size
    }

    override fun onBindViewHolder(holder: UserAdapter.ViewHolder, position: Int) {
        val user = mUser[position]

        holder.userNameTextView.text = user.getUsername()
        holder.userFullnameTextView.text = user.getFullname()
        Picasso.get().load(user.getImage()).placeholder(R.drawable.ic_account_circle_black).into(holder.userProfileImage)

        checkFollowingStatus(user.uid!!, holder.followButton)

        //При нажатии на контейнеp профиля пользователя в поиске откроится фрагмент страницы с его профилем//
        holder.itemView.setOnClickListener (View.OnClickListener {
            if(isFragment)
            {
                val pref = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                pref.putString("profileId", user.uid)
                pref.apply()

                (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.placeHolder, UserFragment()).commit()
            }
            else
            {
                val intent = Intent(mContext, MainActivity::class.java)
                intent.putExtra("publisher", user.uid)
                mContext.startActivity(intent)
            }
        })
        ////////////////////////////////////////////////////////////////////////////////////////////////////


        //////////////////////////При нажатии на кнопку подписаться происходит следующее//////////////////
        holder.followButton.setOnClickListener {
            if(holder.followButton.text.toString() == "Подписаться"){

                firebaseUser?.uid.let { it ->
                    FirebaseDatabase.getInstance().reference
                        .child("Follow").child(it.toString())
                        .child("Following").child(user.uid!!)
                        .setValue(true).addOnCompleteListener { task ->
                            if(task.isSuccessful){

                                firebaseUser?.uid.let { it ->
                                    FirebaseDatabase.getInstance().reference
                                        .child("Follow").child(user.uid!!)
                                        .child("Followers").child(it.toString())
                                        .setValue(true).addOnCompleteListener { task ->
                                            if(task.isSuccessful){

                                            }
                                        }
                                }
                            }
                        }
                }
                addNotification(user.uid!!)
            }
            else{
                firebaseUser?.uid.let { it ->
                    FirebaseDatabase.getInstance().reference
                        .child("Follow").child(it.toString())
                        .child("Following").child(user.uid!!)
                        .removeValue().addOnCompleteListener { task ->
                            if(task.isSuccessful){

                                firebaseUser?.uid.let { it ->
                                    FirebaseDatabase.getInstance().reference
                                        .child("Follow").child(user.uid!!)
                                        .child("Followers").child(it.toString())
                                        .removeValue().addOnCompleteListener { task ->
                                            if(task.isSuccessful){

                                            }
                                        }
                                }
                            }
                        }
                }
            }
        }

    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    class ViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {

        var userNameTextView: TextView = itemView.findViewById(R.id.user_name_search)
        var userFullnameTextView: TextView = itemView.findViewById(R.id.user_full_name_search)
        var userProfileImage: CircleImageView = itemView.findViewById(R.id.user_profile_image_search)
        var followButton: Button = itemView.findViewById(R.id.follow_btn_search)

    }

    private fun checkFollowingStatus(uid: String, followButton: Button) {
        val followingRef = firebaseUser?.uid.let { it ->
            FirebaseDatabase.getInstance().reference
                .child("Follow").child(it.toString())
                .child("Following")
        }

        followingRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(datasnapshot: DataSnapshot)
            {
                if(datasnapshot.child(uid).exists())
                {
                    followButton.text = "Отписаться"
                }
                else
                {
                    followButton.text = "Подписаться"
                }
            }

            override fun onCancelled(error: DatabaseError)
            {

            }
        })
    }

    private fun addNotification(userId: String)
    {
        val notiRef = FirebaseDatabase.getInstance()
            .reference.child("Notifications")
            .child(userId)

        val notiMap = HashMap<String, Any>()
        notiMap["userId"] = firebaseUser!!.uid
        notiMap["text"] = "Подписался на Вас!"
        notiMap["postId"] = ""
        notiMap["isPost"] = false

        notiRef.push().setValue(notiMap)
    }
}

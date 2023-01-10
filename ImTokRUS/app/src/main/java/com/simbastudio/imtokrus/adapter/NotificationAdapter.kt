package com.simbastudio.imtokrus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.fragment.UserFragment
import com.simbastudio.imtokrus.fragments.PostDetailsFragment
import com.simbastudio.imtokrus.model.Notification
import com.simbastudio.imtokrus.model.Post
import com.simbastudio.imtokrus.model.User
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.w3c.dom.Text

class NotificationAdapter(private val mContext: Context, private val mNotification: List<Notification>): RecyclerView.Adapter<NotificationAdapter.ViewHolder>()
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val view = LayoutInflater.from(mContext).inflate(R.layout.notifications_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int
    {
        return mNotification.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val notification = mNotification[position]

        if(notification.getText().equals("Подписался на Вас!"))
        {
            holder.text.text = "Подписался на Вас!"
        }
        else if(notification.getText().equals("Лайкнул Ваш пост"))
        {
            holder.text.text = "Лайкнул Ваш пост"
        }
        else if(notification.getText().contains("Прокоментировал:"))
        {
            holder.text.text = notification.getText().replace("Прокоментировал:", "Прокоментировал: ")
        }
        else
        {
            holder.text.text = notification.getText()
        }

        userInfo(holder.profileImage, holder.userName, notification.getUserId())

        if(notification.getIsPost())
        {
            holder.postImage.visibility = View.VISIBLE
            getPostImage(holder.postImage, notification.getPostId())
        }
        else
        {
            holder.postImage.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if(notification.getIsPost())
            {
                val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                editor.putString("postId", notification.getPostId())
                editor.apply()
                (mContext as FragmentActivity).getSupportFragmentManager().beginTransaction().replace(R.id.placeHolder, PostDetailsFragment()).commit()
            }
            else
            {
                val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                editor.putString("profileId", notification.getUserId())
                editor.apply()
                (mContext as FragmentActivity).getSupportFragmentManager().beginTransaction().replace(R.id.placeHolder, UserFragment()).commit()
            }
        }
    }

    inner class ViewHolder(@NonNull itemView: View): RecyclerView.ViewHolder(itemView)
    {
        var postImage: ImageView
        var profileImage: CircleImageView
        var userName: TextView
        var text: TextView

        init {
            postImage = itemView.findViewById(R.id.notification_post_image)
            profileImage = itemView.findViewById(R.id.notification_profile_image)
            userName = itemView.findViewById(R.id.username_notification)
            text = itemView.findViewById(R.id.comment_notification)
        }
    }

    private fun userInfo(imageView: ImageView, userName: TextView, publisherId: String)
    {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(publisherId)

        usersRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val user = snapshot.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.ic_account_circle_black).into(imageView)

                    userName.text = user!!.getUsername()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getPostImage(imageView: ImageView, postID: String)
    {
        val postRef = FirebaseDatabase.getInstance()
            .reference.child("Posts")
            .child(postID)

        postRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val post = snapshot.getValue<Post>(Post::class.java)

                    Picasso.get().load(post!!.getPostContent()).placeholder(R.drawable.ic_account_circle_black).into(imageView)
                }
            }
            override fun onCancelled(error: DatabaseError)
            {

            }
        })
    }
}
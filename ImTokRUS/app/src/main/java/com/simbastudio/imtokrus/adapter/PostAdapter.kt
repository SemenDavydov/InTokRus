package com.simbastudio.imtokrus.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.activity.CommentsActivity
import com.simbastudio.imtokrus.activity.DetailActivity
import com.simbastudio.imtokrus.activity.MainActivity
import com.simbastudio.imtokrus.activity.ShowUsersActivity
import com.simbastudio.imtokrus.model.Post
import com.simbastudio.imtokrus.model.User
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(private val mContext: Context, private val mPost: List<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>()
{
    private var firebaseUser: FirebaseUser? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.post_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mPost.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        FirebaseAuth.getInstance().currentUser.also { firebaseUser = it }

        val post = mPost[position]

        if(post.postcategory == 2){
            holder.postVideo.isVisible = true
            holder.postVideo.setVideoPath(post.getPostContent())
            holder.postVideo.requestFocus()
            holder.postVideo.start()
            holder.postImage.isVisible = false
        }else{
            holder.postImage.isVisible = true
            Glide.with(mContext)
                .load(post.getPostContent())
                .into(holder.postImage);
            holder.postVideo.isVisible = false
        }

        if(post.description.equals(""))
        {
            holder.description.visibility = View.GONE
        }
        else
        {
            holder.description.visibility = View.VISIBLE
            holder.description.setText(post.description)
        }

        authorInfo(holder.profileImage, holder.userName, holder.publisher, post.publisher!!)

        isLikes(post.id!!, holder.likeButton)

        numberOfLikes(holder.likes, post.id!!)

        getTotalComments(holder.comments, post.id!!)

        counterOfComments(holder.commentsCounter, post.id!!)

        checkSavedStatus(post.id!!, holder.saveButton)

        holder.postImage.setOnClickListener(View.OnClickListener {
            goToDetail(post)
        })
        holder.postVideo.setOnClickListener(View.OnClickListener {
            goToDetail(post)
        })

        holder.likeButton.setOnClickListener {
            if(holder.likeButton.tag == "Like")
            {
                FirebaseDatabase.getInstance().reference
                    .child("Likes")
                    .child(post.id!!)
                    .child(firebaseUser!!.uid)
                    .setValue(true)

                addNotification(post.publisher!!, post.id!!)
            }
            else
            {
                FirebaseDatabase.getInstance().reference
                    .child("Likes")
                    .child(post.id!!)
                    .child(firebaseUser!!.uid)
                    .removeValue()

                val intent = Intent(mContext, MainActivity::class.java)
                mContext.startActivity(intent)
            }
        }

        holder.likes.setOnClickListener {
            val intent = Intent(mContext, ShowUsersActivity::class.java)
            intent.putExtra("id", post.id!!)
            intent.putExtra("title", "likes")
            mContext.startActivity(intent)
        }

        holder.commentButton.setOnClickListener {
            val intentComment = Intent(mContext, CommentsActivity::class.java)
            intentComment.putExtra("postId", post.id!!)
            intentComment.putExtra("publisherId", post.publisher)
            mContext.startActivity(intentComment)
        }

        holder.comments.setOnClickListener {
            val intentComment = Intent(mContext, CommentsActivity::class.java)
            intentComment.putExtra("postId", post.id!!)
            intentComment.putExtra("publisherId", post.publisher)
            mContext.startActivity(intentComment)
        }

        holder.saveButton.setOnClickListener {
            if(holder.saveButton.tag == "Save")
            {
                FirebaseDatabase.getInstance().reference
                    .child("Saves").child(firebaseUser!!.uid)
                    .child(post.id!!)
                    .setValue(true)
            }
            else
            {
                FirebaseDatabase.getInstance().reference
                    .child("Saves").child(firebaseUser!!.uid)
                    .child(post.id!!)
                    .removeValue()
            }
        }
    }

    private fun numberOfLikes(likes: TextView, postid: String)
    {
        val LikesRef = FirebaseDatabase.getInstance().reference
            .child("Likes").child(postid)

        LikesRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    likes.text = snapshot.childrenCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun counterOfComments(counter: TextView, postid: String){
        val counterRes = FirebaseDatabase.getInstance().reference
            .child("Comments").child(postid)

        counterRes.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    counter.text = snapshot.childrenCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getTotalComments(comments: TextView, postid: String)
    {
        val commentsRef = FirebaseDatabase.getInstance().reference
            .child("Comments").child(postid)

        commentsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    comments.text = "Показать все комментарии"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun isLikes(postid: String, likeButton: ImageView)
    {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        val LikesRef = FirebaseDatabase.getInstance().reference
            .child("Likes").child(postid)

        LikesRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(firebaseUser!!.uid).exists())
                {
                    likeButton.setImageResource(R.drawable.like_clicked_btn)
                    likeButton.tag = "Liked"
                }
                else
                {
                    likeButton.setImageResource(R.drawable.not_liked_btn)
                    likeButton.tag = "Like"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun goToDetail(post: Post?) {
        if (post?.content == null) {
            return
        }
        val intent = Intent(this.mContext, DetailActivity::class.java)
        intent.putExtra("postContent", post.content)
        intent.putExtra("postCategory", post.postcategory)
        this.mContext.startActivity(intent)
    }

    inner class ViewHolder(@NonNull itemView: View): RecyclerView.ViewHolder(itemView)
    {
        val profileImage: CircleImageView = itemView.findViewById(R.id.user_profile_image_post)
        val postImage: ImageView = itemView.findViewById(R.id.postContentIv)
        val postVideo: VideoView = itemView.findViewById(R.id.postContentVv)
        val likeButton: ImageView = itemView.findViewById(R.id.post_image_like_btn)
        val commentButton: ImageView = itemView.findViewById(R.id.post_image_comment_btn)
        val saveButton: ImageView = itemView.findViewById(R.id.post_save_comment_btn)
        val userName: TextView = itemView.findViewById(R.id.user_name_post)
        val likes: TextView = itemView.findViewById(R.id.likes)
        val publisher: TextView = itemView.findViewById(R.id.publisher)
        val description: TextView = itemView.findViewById(R.id.description)
        val comments: TextView = itemView.findViewById(R.id.comments)
        val commentsCounter: TextView = itemView.findViewById(R.id.commentsCounter)

        /*init {
            profileImage = itemView.findViewById(R.id.user_profile_image_post)
            postImage = itemView.findViewById(R.id.postContentIv)
            postVideo = itemView.findViewById(R.id.postContentVv)
            likeButton = itemView.findViewById(R.id.post_image_like_btn)
            commentButton = itemView.findViewById(R.id.post_image_comment_btn)
            saveButton = itemView.findViewById(R.id.post_save_comment_btn)
            userName = itemView.findViewById(R.id.user_name_post)
            likes = itemView.findViewById(R.id.likes)
            publisher = itemView.findViewById(R.id.publisher)
            description = itemView.findViewById(R.id.description)
            comments = itemView.findViewById(R.id.comments)
            commentsCounter = itemView.findViewById(R.id.commentsCounter)
        }*/
    }

    private fun authorInfo(profileImage: CircleImageView, userName: TextView, publisher: TextView, publisherID: String) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(publisherID)

        usersRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot)
            {
                if(p0.exists())
                {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.ic_account_circle_black).into(profileImage)
                    userName.text = user.getUsername()
                    publisher.text = user.getFullname()

                }
            }

            override fun onCancelled(p0: DatabaseError)
            {

            }
        })
    }

    private fun checkSavedStatus(postid: String, imageView: ImageView)
    {
        val savesRef = FirebaseDatabase.getInstance().reference
            .child("Saves")
            .child(firebaseUser!!.uid)

        savesRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(postid).exists())
                {
                    imageView.setImageResource(R.drawable.ic_bookmark_saved)
                    imageView.tag = "Saved"
                }
                else
                {
                    imageView.setImageResource(R.drawable.save_posts_in_favs)
                    imageView.tag = "Save"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addNotification(userId: String, postId: String)
    {
        val notiRef = FirebaseDatabase.getInstance()
            .reference.child("Notifications")
            .child(userId)

        val notiMap = HashMap<String, Any>()
        notiMap["userId"] = firebaseUser!!.uid
        notiMap["text"] = "Лайкнул Ваш пост"
        notiMap["postId"] = postId
        notiMap["isPost"] = true

        notiRef.push().setValue(notiMap)
    }
}
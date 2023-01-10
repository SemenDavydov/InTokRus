package com.simbastudio.imtokrus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.fragments.PostDetailsFragment
import com.simbastudio.imtokrus.model.Post
import com.squareup.picasso.Picasso

class MyImagesAdapter(private val mContext: Context, mPost: List<Post>): RecyclerView.Adapter<MyImagesAdapter.ViewHolder?>() {

    private var mPost: List<Post>? = null


    init {
        this.mPost = mPost
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.images_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mPost!!.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post: Post = mPost!![position]
        Glide.with(mContext)
            .load(post.getPostContent())
            .into(holder.postImage);

        holder.postImage.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("postId", post.id)
            editor.apply()
            (mContext as FragmentActivity).getSupportFragmentManager().beginTransaction().replace(R.id.placeHolder, PostDetailsFragment()).commit()
        }
    }

    inner class ViewHolder(@NonNull itemView: View)
        : RecyclerView.ViewHolder(itemView)
    {
        var postImage: ImageView

        init {
            postImage = itemView.findViewById(R.id.post_image)
        }
    }
}
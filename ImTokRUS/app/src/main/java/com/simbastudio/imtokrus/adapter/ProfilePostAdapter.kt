package com.simbastudio.imtokrus.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.activity.DetailActivity
import com.simbastudio.imtokrus.model.Post

class ProfilePostAdapter(private val context: Context, private val posts: List<Post>) : RecyclerView.Adapter<ProfilePostAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_post_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        if (post.postcategory == 2) {
            holder.postContentVv.isVisible = true
            holder.postContentIv.isVisible = false
            holder.postContentVv.setVideoPath(post.getPostContent())
            holder.postContentVv.requestFocus()
            holder.postContentVv.start()
        } else if (post.postcategory == 1) {
            holder.postContentVv.isVisible = false
            holder.postContentIv.isVisible = true
            Glide.with(context)
                .load(post.getPostContent())
                .into(holder.postContentIv);
        }
        holder.postItemFl.setOnClickListener(View.OnClickListener {
            goToDetail(post)
        })
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val postContentIv: ImageView = itemView.findViewById(R.id.postContentIv1)
        val postContentVv: VideoView = itemView.findViewById(R.id.postContentVv1)
        val postItemFl: FrameLayout = itemView.findViewById(R.id.postItemFl)
    }

    private fun goToDetail(post: Post?) {
        if (post?.getPostContent() == null) {
            return
        }
        val intent = Intent(this.context, DetailActivity::class.java)
        intent.putExtra("postContent", post.getPostContent())
        intent.putExtra("postCategory", post.postcategory)
        this.context.startActivity(intent)
    }
}
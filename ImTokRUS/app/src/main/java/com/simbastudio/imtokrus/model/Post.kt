package com.simbastudio.imtokrus.model

class Post {


    var publisher:  String? = null
    var description:  String? = null
    var id: String? = null
    var postcategory: Int? = null

    private var postContent: String = ""


    constructor()

    constructor(postContent: String){
        this.postContent = postContent

    }

    fun getPostContent() : String {
        return postContent
    }

    fun setPostContent(postContent: String){
        this.postContent = postContent
    }

}




package com.facedetector.extension

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("src")
fun ImageView.setImage(path: String) {
    Glide.with(this)
        .load(path)
        .into(this)
}
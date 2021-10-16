package com.manuel.red.detail

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.storage.StorageReference
import com.manuel.red.R

class DetailAdapter(
    private val imgList: MutableList<StorageReference>,
    private val context: Context
) : PagerAdapter() {
    override fun getCount(): Int = imgList.size
    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imgPackageService = ShapeableImageView(context)
        GlideApp.with(context).load(imgList[position]).diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_error_outline)
            .centerCrop().into(imgPackageService)
        container.addView(imgPackageService, 0)
        return imgPackageService
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ShapeableImageView)
    }
}
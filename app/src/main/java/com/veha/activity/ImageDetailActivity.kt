package com.veha.activity

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target


class ImageDetailActivity : AppCompatActivity() {
    var imgPath: String? = null
    private lateinit var imageView: SubsamplingScaleImageView
    private var scaleGestureDetector: ScaleGestureDetector? = null

    private var mScaleFactor = 1.0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)
        imgPath = intent.getStringExtra("profilePic")
        imageView = findViewById(R.id.idIVImage)
//        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        if (imgPath != null) {
            Picasso.get().load(imgPath).into(object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    if (bitmap != null) {
                        // Set the loaded Bitmap on the SubsamplingScaleImageView
                        imageView.setImage(ImageSource.bitmap(bitmap))
                    }
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    // Handle error if image loading fails
                    e?.printStackTrace()
                }

                override fun onPrepareLoad(placeHolder: Drawable?) {
                    // Optionally, show a placeholder while loading
                }
            })
//            Picasso.get()
//                .load(imgPath)
//                .placeholder(android.R.drawable.progress_indeterminate_horizontal) // Optional placeholder
//                .error(android.R.drawable.stat_notify_error) // Optional error icon
//                .into(imageView)
           // Picasso.with(this).load(imgPath).into(imageView)
        }
    }

//    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
//        scaleGestureDetector!!.onTouchEvent(motionEvent)
//        return true
//    }
//
//    private inner class ScaleListener : SimpleOnScaleGestureListener() {
//        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
//            mScaleFactor *= scaleGestureDetector.scaleFactor
//            mScaleFactor = 0.1f.coerceAtLeast(mScaleFactor.coerceAtMost(10.0f))
//            imageView!!.scaleX = mScaleFactor
//            imageView!!.scaleY = mScaleFactor
//            return true
//        }
//    }
}

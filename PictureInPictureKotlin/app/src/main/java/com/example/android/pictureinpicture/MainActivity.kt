package com.example.android.pictureinpicture

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            adjustFullScreen(resources.configuration)
        }
    }

    /**
     * Adjusts immersive full-screen flags depending on the screen orientation.

     * @param config The current [Configuration].
     */
    private fun adjustFullScreen(config: Configuration) {
        val insetsController = ViewCompat.getWindowInsetsController(window.decorView)
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
//            binding.scroll.visibility = View.GONE
//            binding.movie.setAdjustViewBounds(false)
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
//            binding.scroll.visibility = View.VISIBLE
//            binding.movie.setAdjustViewBounds(true)
        }
    }
}
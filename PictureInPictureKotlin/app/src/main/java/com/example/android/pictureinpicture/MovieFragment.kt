/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pictureinpicture

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.util.Linkify
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.example.android.pictureinpicture.databinding.FragmentMovieBinding
import com.example.android.pictureinpicture.widget.MovieView

/**
 * Demonstrates usage of Picture-in-Picture when using [MediaSessionCompat].
 */
class MovieFragment : Fragment(R.layout.fragment_movie) {

    private var _binding: FragmentMovieBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: MediaSessionCompat

    /**
     * Callbacks from the [MovieView] showing the video playback.
     */
    private val movieListener = object : MovieView.MovieListener() {

        override fun onMovieStarted() {
            // We are playing the video now. Update the media session state and the PiP window will
            // update the actions.
            updatePlaybackState(
                PlaybackStateCompat.STATE_PLAYING,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }

        override fun onMovieStopped() {
            // The video stopped or reached its end. Update the media session state and the PiP
            // window will update the actions.
            updatePlaybackState(
                PlaybackStateCompat.STATE_PAUSED,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }

        override fun onMovieMinimized() {
            // The MovieView wants us to minimize it. We enter Picture-in-Picture mode now.
            minimize()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.run {
            Linkify.addLinks(explanation, Linkify.ALL)
            pip.setOnClickListener { minimize() }
            switchExample.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            // Configure parameters for the picture-in-picture mode. We do this at the first layout of
            // the MovieView because we use its layout position and size.
            movie.doOnLayout { updatePictureInPictureParams() }

            // Set up the video; it automatically starts.
            movie.setMovieListener(movieListener)
            back.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        session = MediaSessionCompat(requireActivity(), TAG)
        session.isActive = true
        MediaControllerCompat.setMediaController(requireActivity(), session.controller)

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, binding.movie.title)
            .build()
        session.setMetadata(metadata)
        session.setCallback(MediaSessionCallback(binding.movie))

        val state = if (binding.movie.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        updatePlaybackState(
            state,
            MEDIA_ACTIONS_ALL,
            binding.movie.getCurrentPosition(),
            binding.movie.getVideoResourceId()
        )
    }

    override fun onStop() {
        super.onStop()
        // On entering Picture-in-Picture mode, onPause is called, but not onStop.
        // For this reason, this is the place where we should pause the video playback.
        binding.movie.pause()
        session.release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (!requireActivity().isInPictureInPictureMode) {
            // Show the video controls so the video can be easily resumed.
            binding.movie.showControls()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(newConfig)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            // Hide the controls in picture-in-picture mode.
            binding.movie.hideControls()
        } else {
            // Show the video controls if the video is not playing
            if (!binding.movie.isPlaying) {
                binding.movie.showControls()
            }
        }
    }

    private fun updatePictureInPictureParams(): PictureInPictureParams {
        // Calculate the aspect ratio of the PiP screen.
        val aspectRatio = Rational(binding.movie.width, binding.movie.height)
        // The movie view turns into the picture-in-picture mode.
        val visibleRect = Rect()
        binding.movie.getGlobalVisibleRect(visibleRect)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            // Specify the portion of the screen that turns into the picture-in-picture mode.
            // This makes the transition animation smoother.
            .setSourceRectHint(visibleRect)
            // The screen automatically turns into the picture-in-picture mode when it is hidden
            // by the "Home" button.
            .setAutoEnterEnabled(true)
            .build()
        requireActivity().setPictureInPictureParams(params)
        return params
    }

    /**
     * Enters Picture-in-Picture mode.
     */
    private fun minimize() {
        requireActivity().enterPictureInPictureMode(updatePictureInPictureParams())
    }

    /**
     * Adjusts immersive full-screen flags depending on the screen orientation.

     * @param config The current [Configuration].
     */
    private fun adjustFullScreen(config: Configuration) {
        val insetsController =
            ViewCompat.getWindowInsetsController(requireActivity().window.decorView)
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            binding.scroll.visibility = View.GONE
            binding.movie.setAdjustViewBounds(false)
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            binding.scroll.visibility = View.VISIBLE
            binding.movie.setAdjustViewBounds(true)
        }
    }

    /**
     * Overloaded method that persists previously set media actions.

     * @param state The state of the video, e.g. playing, paused, etc.
     * @param position The position of playback in the video.
     * @param mediaId The media id related to the video in the media session.
     */
    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        position: Int,
        mediaId: Int
    ) {
        val actions = session.controller.playbackState.actions
        updatePlaybackState(state, actions, position, mediaId)
    }

    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        playbackActions: Long,
        position: Int,
        mediaId: Int
    ) {
        val builder = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setActiveQueueItemId(mediaId.toLong())
            .setState(state, position.toLong(), 1.0f)
        session.setPlaybackState(builder.build())
    }

    /**
     * Updates the [MovieView] based on the callback actions. <br></br>
     * Simulates a playlist that will disable actions when you cannot skip through the playlist in a
     * certain direction.
     */
    private inner class MediaSessionCallback(
        private val movieView: MovieView
    ) : MediaSessionCompat.Callback() {

        private var indexInPlaylist: Int = 1

        override fun onPlay() {
            movieView.play()
        }

        override fun onPause() {
            movieView.pause()
        }

        override fun onSkipToNext() {
            movieView.startVideo()
            if (indexInPlaylist < PLAYLIST_SIZE) {
                indexInPlaylist++
                if (indexInPlaylist >= PLAYLIST_SIZE) {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                } else {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_ALL,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                }
            }
        }

        override fun onSkipToPrevious() {
            movieView.startVideo()
            if (indexInPlaylist > 0) {
                indexInPlaylist--
                if (indexInPlaylist <= 0) {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                } else {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_ALL,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "MediaSessionPlaybackFragment"
        private const val MEDIA_ACTIONS_PLAY_PAUSE =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
        private const val MEDIA_ACTIONS_ALL =
            MEDIA_ACTIONS_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        private const val PLAYLIST_SIZE = 2

        fun newInstance() = MovieFragment()
    }
}

package com.example.ui.screens.classroom

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

/**
 * High-performance hardware-accelerated LiveKit WebRTC video renderer for Jetpack Compose.
 * Connects directly to WebRTC hardware decoder and renders onto TextureView GPU surface.
 * Includes window attach guards to eliminate EGL surface race conditions on rotation/PiP.
 */
@Composable
fun LiveKitVideoRendererView(
    room: Room?,
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    AndroidView(
        factory = { ctx ->
            TextureViewRenderer(ctx).apply {
                try {
                    room?.initVideoRenderer(this)
                    setMirror(mirror)
                    setEnableHardwareScaler(true)
                } catch (e: Exception) {
                    android.util.Log.w("LiveKitRenderer", "init error: ${e.message}")
                }

                // Window attach guard: unbind track on window detach to prevent EGL surface deadlock
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        try {
                            val track = tag as? VideoTrack
                            track?.addRenderer(this@apply)
                        } catch (_: Exception) {}
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        try {
                            val track = tag as? VideoTrack
                            track?.removeRenderer(this@apply)
                        } catch (_: Exception) {}
                    }
                })
            }
        },
        update = { renderer ->
            try {
                renderer.setMirror(mirror)
                val prevTrack = renderer.tag as? VideoTrack
                if (prevTrack != videoTrack) {
                    prevTrack?.removeRenderer(renderer)
                    renderer.tag = videoTrack
                    if (renderer.isAttachedToWindow) {
                        videoTrack?.addRenderer(renderer)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("LiveKitRenderer", "update error: ${e.message}")
            }
        },
        onRelease = { renderer ->
            try {
                val prevTrack = renderer.tag as? VideoTrack
                prevTrack?.removeRenderer(renderer)
                renderer.tag = null
                renderer.release()
            } catch (_: Exception) {}
        },
        modifier = modifier
    )
}

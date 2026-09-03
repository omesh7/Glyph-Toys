package dev.omesh.glyphtoys.toys

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import dev.omesh.glyphtoys.glyph.GlyphToyService
import dev.omesh.glyphtoys.glyph.MatrixCanvas
import dev.omesh.glyphtoys.glyph.Tone
import dev.omesh.glyphtoys.media.GlyphNotificationListener
import kotlin.math.sin

/**
 * Album art of whatever is playing, on the matrix.
 *
 * The art is tone-mapped once per track by [Tone] — averaged in linear light, contrast stretched
 * to use all 137 LEDs — and then just held. Nothing moves, so this renders at 5fps rather than 30
 * to stay honest about the battery cost of an always-on toy.
 *
 * Needs notification access; see [GlyphNotificationListener].
 */
class AlbumArtToyService : GlyphToyService() {

    /** Static image, so there is nothing to gain from a fast loop. */
    override val frameIntervalMs = 200L

    private val sessions by lazy { getSystemService(MediaSessionManager::class.java) }
    private val listener by lazy { ComponentName(this, GlyphNotificationListener::class.java) }

    private var pixels: FloatArray? = null
    private var showingTrack: String? = null
    private var playing = false
    private var lastPollMs = Long.MIN_VALUE

    override fun draw(canvas: MatrixCanvas, elapsedMs: Long) {
        poll(elapsedMs)

        val art = pixels
        when {
            art != null -> canvas.image(art)

            // Playing, but the track carries no artwork. A slow pulse beats a dead panel.
            playing -> {
                val c = (geometry.size - 1) / 2f
                val pulse = 0.35f + 0.25f * sin(elapsedMs / 700.0).toFloat()
                canvas.disc(c, c, geometry.size / 6f, pulse)
            }

            // Nothing playing: stay dark rather than burn LEDs on an always-on display.
        }
    }

    private fun poll(elapsedMs: Long) {
        if (elapsedMs - lastPollMs < POLL_INTERVAL_MS) return
        lastPollMs = elapsedMs

        if (!GlyphNotificationListener.isEnabled(this)) {
            clear()
            return
        }

        val controller = runCatching {
            sessions?.getActiveSessions(listener)
                ?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        }.onFailure { Log.w("AlbumArtToy", "no media session access: ${it.message}") }
            .getOrNull()

        if (controller == null) {
            clear()
            return
        }
        playing = true

        val metadata = controller.metadata
        val track = metadata?.let {
            it.getString(MediaMetadata.METADATA_KEY_TITLE) + "|" +
                it.getString(MediaMetadata.METADATA_KEY_ARTIST)
        }
        if (track == showingTrack) return
        showingTrack = track

        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        pixels = art?.let { toMatrix(it) }
    }

    private fun clear() {
        playing = false
        pixels = null
        showingTrack = null
    }

    /** Album art to one perceptual value per LED. Runs once per track, off the main thread. */
    private fun toMatrix(art: Bitmap): FloatArray? = runCatching {
        // Bound the intermediate allocation. The box filter below averages many source pixels per
        // cell anyway, so a first reduction here costs almost nothing visually.
        val source = if (maxOf(art.width, art.height) > MAX_SOURCE_EDGE) {
            val ratio = MAX_SOURCE_EDGE.toFloat() / maxOf(art.width, art.height)
            Bitmap.createScaledBitmap(
                art,
                (art.width * ratio).toInt().coerceAtLeast(1),
                (art.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            art
        }

        val argb = IntArray(source.width * source.height)
        source.getPixels(argb, 0, source.width, 0, 0, source.width, source.height)
        if (source !== art) source.recycle()

        Tone.downscale(argb, source.width, source.height, geometry.size)
            .also { Tone.autoContrast(it) }
    }.onFailure { Log.w("AlbumArtToy", "could not read album art: ${it.message}") }.getOrNull()

    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
        const val MAX_SOURCE_EDGE = 512
    }
}

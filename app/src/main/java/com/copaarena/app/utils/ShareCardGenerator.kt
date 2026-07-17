package com.copaarena.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareCardGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generateAndShare(
        tournamentName: String,
        winnerId: Long?,
        standings: List<TournamentStatsEntity>,
        players: List<PlayerEntity>
    ) {
        try {
            val bitmap = renderCard(tournamentName, winnerId, standings, players)
            val uri = saveToMediaStore(bitmap) ?: return
            shareImage(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate share card")
        }
    }

    private fun renderCard(tournamentName: String, winnerId: Long?, standings: List<TournamentStatsEntity>, players: List<PlayerEntity>): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0D1B2A"))

        val gold = Color.parseColor("#EF9F27")
        val white = Color.WHITE
        val faded = Color.parseColor("#B0C4D4")

        val logoPaint = Paint().apply {
            color = gold
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("COPA ARENA", width / 2f, 140f, logoPaint)

        val namePaint = Paint(logoPaint).apply { color = white; textSize = 44f }
        canvas.drawText(tournamentName.uppercase(), width / 2f, 210f, namePaint)

        val ordered = standings.sortedBy { it.finalPosition ?: Int.MAX_VALUE }
        val championPlayer = players.find { it.id == winnerId }

        val labelPaint = Paint().apply { color = gold; textSize = 34f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("CHAMPION", width / 2f, 340f, labelPaint)

        val championPaint = Paint().apply {
            color = white; textSize = 72f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(championPlayer?.name ?: "-", width / 2f, 430f, championPaint)

        val teamPaint = Paint().apply { color = faded; textSize = 32f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(championPlayer?.teamName ?: "", width / 2f, 480f, teamPaint)

        val dividerPaint = Paint().apply { color = Color.argb(40, 255, 255, 255); strokeWidth = 2f }
        canvas.drawLine(80f, 560f, width - 80f, 560f, dividerPaint)

        var y = 640f
        val headerPaint = Paint().apply { color = gold; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("RANK", 80f, y, headerPaint)
        canvas.drawText("PLAYER", 220f, y, headerPaint)
        val ptsX = width - 220f
        val goalsX = width - 100f
        canvas.drawText("PTS", ptsX, y, headerPaint)
        canvas.drawText("GOALS", goalsX, y, headerPaint)
        y += 50f

        val rowPaint = Paint().apply { color = white; textSize = 30f; isAntiAlias = true }
        ordered.forEach { stat ->
            val player = players.find { it.id == stat.playerId }
            canvas.drawText("#${stat.finalPosition ?: "-"}", 80f, y, rowPaint)
            canvas.drawText(player?.name ?: "Unknown", 220f, y, rowPaint)
            canvas.drawText("${stat.points}", ptsX, y, rowPaint)
            canvas.drawText("${stat.goals}", goalsX, y, rowPaint)
            y += 52f
        }

        val footerPaint = Paint().apply { color = faded; textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("Generated with COPA Arena", width / 2f, height - 60f, footerPaint)

        return bitmap
    }

    private fun saveToMediaStore(bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val filename = "copa_arena_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/COPA Arena")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    private fun shareImage(uri: Uri) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Tournament Result").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

package com.example.draghetto

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.os.SystemClock
import kotlin.math.min
import kotlin.random.Random

class DragonGameView(context: Context) : View(context) {
    private enum class State { READY, PLAYING, OVER }

    private data class Pipe(var x: Float, var gapY: Float, var scored: Boolean = false)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipes = mutableListOf<Pipe>()
    private val preferences = context.getSharedPreferences("draghetto", Context.MODE_PRIVATE)
    private var state = State.READY
    private var dragonY = 360f
    private var velocity = 0f
    private var score = 0
    private var best = preferences.getInt("best", 0)
    private var lastFrame = 0L
    private var worldScale = 1f
    private var worldOffsetX = 0f
    private var worldOffsetY = 0f

    private val dragonX = 112f
    private val dragonRadius = 23f
    private val pipeWidth = 72f
    private val pipeGap = 208f
    private val pipeSpacing = 248f
    private val groundY = 738f

    init {
        isClickable = true
        resetGame()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        worldScale = min(width / 400f, height / 800f)
        worldOffsetX = (width - 400f * worldScale) / 2f
        worldOffsetY = (height - 800f * worldScale) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val delta = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1000f).coerceIn(0f, 0.033f)
        lastFrame = now

        canvas.save()
        canvas.translate(worldOffsetX, worldOffsetY)
        canvas.scale(worldScale, worldScale)
        drawBackground(canvas)

        if (state == State.PLAYING) {
            updateGame(delta)
        }
        pipes.forEach { drawPipe(canvas, it) }
        drawGround(canvas)
        drawDragon(canvas, now)
        drawScore(canvas)

        when (state) {
            State.READY -> drawMessage(canvas, "DRAGHETTO RUN", "Tocca per volare!")
            State.OVER -> drawMessage(canvas, "GAME OVER", "Tocca per riprovare")
            State.PLAYING -> Unit
        }
        canvas.restore()

        if (state == State.PLAYING) {
            postInvalidateOnAnimation()
        }
    }

    private fun updateGame(delta: Float) {
        velocity = (velocity + 980f * delta).coerceAtMost(620f)
        dragonY += velocity * delta

        pipes.forEach { it.x -= 175f * delta }
        val lastPipeX = pipes.lastOrNull()?.x ?: 400f
        if (lastPipeX < 400f) {
            pipes += Pipe(lastPipeX + pipeSpacing, Random.nextInt(265, 530).toFloat())
        }
        pipes.removeAll { it.x + pipeWidth < -10f }

        pipes.forEach { pipe ->
            if (!pipe.scored && pipe.x + pipeWidth < dragonX) {
                pipe.scored = true
                score++
                if (score > best) {
                    best = score
                    preferences.edit().putInt("best", best).apply()
                }
            }

            val overlapsDragon = dragonX + dragonRadius > pipe.x &&
                dragonX - dragonRadius < pipe.x + pipeWidth
            val outsideGap = dragonY - dragonRadius < pipe.gapY - pipeGap / 2f ||
                dragonY + dragonRadius > pipe.gapY + pipeGap / 2f
            if (overlapsDragon && outsideGap) {
                endGame()
                return
            }
        }

        if (dragonY - dragonRadius < 0f || dragonY + dragonRadius > groundY) {
            endGame()
        }
    }

    private fun resetGame() {
        score = 0
        dragonY = 360f
        velocity = 0f
        pipes.clear()
        pipes += Pipe(440f, 380f)
        pipes += Pipe(440f + pipeSpacing, 310f)
        state = State.READY
        lastFrame = 0L
    }

    private fun endGame() {
        state = State.OVER
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(Color.rgb(104, 203, 236))

        paint.color = Color.argb(80, 255, 255, 255)
        drawCloud(canvas, 62f, 155f, 1f)
        drawCloud(canvas, 290f, 230f, 0.72f)
        drawCloud(canvas, 210f, 90f, 0.55f)

        paint.color = Color.rgb(115, 205, 157)
        val hills = Path().apply {
            moveTo(0f, groundY)
            lineTo(0f, 610f)
            cubicTo(95f, 520f, 135f, 665f, 235f, 580f)
            cubicTo(300f, 520f, 340f, 565f, 400f, 540f)
            lineTo(400f, groundY)
            close()
        }
        canvas.drawPath(hills, paint)
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float, scale: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        canvas.drawCircle(0f, 0f, 22f, paint)
        canvas.drawCircle(24f, -8f, 29f, paint)
        canvas.drawCircle(52f, 1f, 21f, paint)
        canvas.drawRoundRect(0f, 0f, 52f, 20f, 10f, 10f, paint)
        canvas.restore()
    }

    private fun drawPipe(canvas: Canvas, pipe: Pipe) {
        val top = pipe.gapY - pipeGap / 2f
        val bottom = pipe.gapY + pipeGap / 2f
        paint.color = Color.rgb(37, 161, 98)
        canvas.drawRoundRect(pipe.x + 6f, -8f, pipe.x + pipeWidth - 6f, top, 8f, 8f, paint)
        canvas.drawRoundRect(pipe.x + 6f, bottom, pipe.x + pipeWidth - 6f, groundY + 10f, 8f, 8f, paint)

        paint.color = Color.rgb(25, 133, 77)
        canvas.drawRoundRect(pipe.x, top - 22f, pipe.x + pipeWidth, top + 1f, 8f, 8f, paint)
        canvas.drawRoundRect(pipe.x, bottom - 1f, pipe.x + pipeWidth, bottom + 22f, 8f, 8f, paint)
        paint.color = Color.argb(65, 255, 255, 255)
        canvas.drawRoundRect(pipe.x + 15f, 0f, pipe.x + 23f, top - 24f, 4f, 4f, paint)
        canvas.drawRoundRect(pipe.x + 15f, bottom + 24f, pipe.x + 23f, groundY, 4f, 4f, paint)
    }

    private fun drawGround(canvas: Canvas) {
        paint.color = Color.rgb(239, 199, 110)
        canvas.drawRect(0f, groundY, 400f, 800f, paint)
        paint.color = Color.rgb(119, 190, 79)
        canvas.drawRect(0f, groundY, 400f, groundY + 13f, paint)
        paint.color = Color.rgb(215, 164, 83)
        for (x in 12..400 step 36) {
            canvas.drawCircle(x.toFloat(), groundY + 35f + (x % 3) * 9f, 3f, paint)
        }
    }

    private fun drawDragon(canvas: Canvas, now: Long) {
        val wingLift = if (state == State.PLAYING) kotlin.math.sin(now / 90.0).toFloat() * 8f else 0f
        canvas.save()
        canvas.translate(dragonX, dragonY)

        paint.color = Color.rgb(42, 155, 94)
        val tail = Path().apply {
            moveTo(-17f, 8f)
            lineTo(-39f, -3f)
            lineTo(-34f, 17f)
            lineTo(-17f, 17f)
            close()
        }
        canvas.drawPath(tail, paint)

        paint.color = Color.rgb(58, 190, 112)
        val wing = Path().apply {
            moveTo(-9f, 1f)
            lineTo(-22f, -25f - wingLift)
            lineTo(2f, -19f - wingLift)
            lineTo(12f, -7f)
            close()
        }
        canvas.drawPath(wing, paint)
        canvas.drawOval(RectF(-20f, -17f, 27f, 23f), paint)
        canvas.drawCircle(15f, -9f, 19f, paint)

        paint.color = Color.rgb(248, 180, 91)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        val horns = Path().apply {
            moveTo(3f, -23f)
            lineTo(7f, -37f)
            lineTo(14f, -25f)
            moveTo(17f, -24f)
            lineTo(25f, -34f)
            lineTo(26f, -20f)
        }
        canvas.drawPath(horns, paint)
        paint.style = Paint.Style.FILL
        canvas.drawOval(RectF(15f, -2f, 36f, 8f), paint)

        paint.color = Color.WHITE
        canvas.drawCircle(20f, -14f, 6f, paint)
        paint.color = Color.rgb(38, 50, 56)
        canvas.drawCircle(22f, -14f, 2.8f, paint)

        paint.color = Color.rgb(255, 224, 135)
        canvas.drawOval(RectF(-8f, 7f, 13f, 17f), paint)
        paint.color = Color.rgb(42, 155, 94)
        canvas.drawCircle(-7f, 20f, 5f, paint)
        canvas.drawCircle(13f, 19f, 5f, paint)
        canvas.restore()
    }

    private fun drawScore(canvas: Canvas) {
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
        paint.setShadowLayer(4f, 0f, 3f, Color.argb(100, 0, 0, 0))
        paint.color = Color.WHITE
        paint.textSize = 58f
        canvas.drawText(score.toString(), 200f, 82f, paint)
        paint.clearShadowLayer()
        paint.textSize = 16f
        paint.color = Color.rgb(255, 255, 255)
        canvas.drawText("RECORD  $best", 200f, 108f, paint)
    }

    private fun drawMessage(canvas: Canvas, title: String, subtitle: String) {
        paint.color = Color.argb(155, 22, 54, 72)
        canvas.drawRoundRect(34f, 300f, 366f, 475f, 24f, 24f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
        paint.color = Color.WHITE
        paint.textSize = if (title.length > 12) 29f else 36f
        canvas.drawText(title, 200f, 365f, paint)
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        paint.textSize = 19f
        paint.color = Color.rgb(255, 226, 151)
        canvas.drawText(subtitle, 200f, 413f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (state) {
                State.READY -> {
                    state = State.PLAYING
                    flap()
                }
                State.PLAYING -> flap()
                State.OVER -> {
                    resetGame()
                    state = State.PLAYING
                    flap()
                }
            }
            invalidate()
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
            return true
        }
        return true
    }

    private fun flap() {
        velocity = -355f
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

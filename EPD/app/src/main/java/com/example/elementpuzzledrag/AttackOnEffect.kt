package com.example.elementpuzzledrag

import android.graphics.Canvas
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.AnimSprite
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IRecyclable
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.min
import kotlin.math.roundToInt

class AttackOnEffect private constructor(
    private val gameContext: GameContext,
) : AnimSprite(
    gctx = gameContext,
    resId = R.mipmap.attackon,
    fps = DEFAULT_FPS,
    frameCount = FRAME_COUNT,
), IRecyclable {

    companion object {
        private const val FRAME_COUNT = 27
        private const val FRAME_HEIGHT = 108f
        private const val DEFAULT_FPS = 27f

        private const val DRAW_SIZE_SCALE = 1.35f
        private const val DRAW_HEIGHT_SCALE = 4.0f
        private const val CENTER_Y_OFFSET_RATIO = -0.50f

        fun get(
            gctx: GameContext,
            world: World<Layer>,
            slot: ElementSlot,
            durationSeconds: Float,
            onFinished: () -> Unit = {},
        ): AttackOnEffect {
            val effect = world.obtain(AttackOnEffect::class.java)
                ?: AttackOnEffect(gctx)

            return effect.init(
                world = world,
                slot = slot,
                durationSeconds = durationSeconds,
                onFinished = onFinished,
            )
        }
    }

    private lateinit var world: World<Layer>

    private var elapsed = 0f
    private var duration = 0f
    private var finished = false
    private var onFinished: (() -> Unit)? = null

    private fun init(
        world: World<Layer>,
        slot: ElementSlot,
        durationSeconds: Float,
        onFinished: () -> Unit,
    ): AttackOnEffect {
        this.world = world
        this.onFinished = onFinished

        bitmap = gameContext.res.getBitmap(R.mipmap.attackon)
        frameCount = FRAME_COUNT

        elapsed = 0f
        duration = durationSeconds.coerceAtLeast(0.01f)
        fps = FRAME_COUNT / duration
        finished = false

        val frameWidth = bitmap.width.toFloat() / FRAME_COUNT
        val baseSize = min(slot.slotWidth, slot.slotHeight)

        val drawWidth = baseSize * DRAW_SIZE_SCALE
        val drawHeight = drawWidth * FRAME_HEIGHT / frameWidth * DRAW_HEIGHT_SCALE

        val centerX = slot.slotLeft + slot.slotWidth / 2f
        val centerY = slot.slotTop + slot.slotHeight / 2f +
                slot.slotHeight * CENTER_Y_OFFSET_RATIO

        setSize(drawWidth, drawHeight)
        setCenter(centerX, centerY)

        return this
    }

    override fun update(gctx: GameContext) {
        if (finished) return

        elapsed += gctx.frameTime

        if (elapsed >= duration) {
            finished = true

            val callback = onFinished

            world.remove(this, Layer.SKILL_UI)
            callback?.invoke()
        }
    }

    override fun draw(canvas: Canvas) {
        if (finished) return

        syncDstRect()

        val frameIndex = ((elapsed * fps).toInt())
            .coerceIn(0, FRAME_COUNT - 1)

        val frameLeft = (bitmap.width * frameIndex / FRAME_COUNT.toFloat()).roundToInt()
        val frameRight = (bitmap.width * (frameIndex + 1) / FRAME_COUNT.toFloat()).roundToInt()

        srcRect?.set(
            frameLeft,
            0,
            frameRight,
            bitmap.height,
        )

        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
    }

    override fun onRecycle() {
        elapsed = 0f
        duration = 0f
        finished = false
        onFinished = null
    }
}
package com.example.elementpuzzledrag

import android.graphics.Canvas
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.AnimSprite
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IRecyclable
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

enum class AttackEffectKind {
    SMALL,
    NORMAL1,
    NORMAL2,
    BIG,
}

class AttackEffect private constructor(
    private val gameContext: GameContext,
) : AnimSprite(
    gctx = gameContext,
    resId = R.mipmap.effect_small,
    fps = EFFECT_FPS,
    frameCount = 16,
), IRecyclable {

    companion object {
        private const val EFFECT_FPS = 30f

        fun get(
            gctx: GameContext,
            world: World<Layer>,
            kind: AttackEffectKind,
            centerX: Float,
            centerY: Float,
            onFinished: () -> Unit = {},
        ): AttackEffect {
            val effect = world.obtain(AttackEffect::class.java)
                ?: AttackEffect(gctx)

            return effect.init(
                world = world,
                kind = kind,
                centerX = centerX,
                centerY = centerY,
                onFinished = onFinished,
            )
        }

        private fun effectResId(kind: AttackEffectKind): Int {
            return when (kind) {
                AttackEffectKind.SMALL -> R.mipmap.effect_small
                AttackEffectKind.NORMAL1 -> R.mipmap.effect_normal1
                AttackEffectKind.NORMAL2 -> R.mipmap.effect_normal2
                AttackEffectKind.BIG -> R.mipmap.effect_big
            }
        }

        private fun effectFrameCount(kind: AttackEffectKind): Int {
            return when (kind) {
                AttackEffectKind.SMALL -> 16
                AttackEffectKind.NORMAL1 -> 25
                AttackEffectKind.NORMAL2 -> 19
                AttackEffectKind.BIG -> 11
            }
        }

        private fun effectFrameWidth(kind: AttackEffectKind): Float {
            return when (kind) {
                AttackEffectKind.SMALL -> 1840f / 16f
                AttackEffectKind.NORMAL1 -> 2775f / 25f
                AttackEffectKind.NORMAL2 -> 3021f / 19f
                AttackEffectKind.BIG -> 6721f / 11f
            }
        }

        private fun effectFrameHeight(kind: AttackEffectKind): Float {
            return when (kind) {
                AttackEffectKind.SMALL -> 158f
                AttackEffectKind.NORMAL1 -> 123f
                AttackEffectKind.NORMAL2 -> 223f
                AttackEffectKind.BIG -> 373f
            }
        }

        private fun effectDrawWidth(kind: AttackEffectKind): Float {
            return when (kind) {
                AttackEffectKind.SMALL -> 150f
                AttackEffectKind.NORMAL1,
                AttackEffectKind.NORMAL2 -> 300f
                AttackEffectKind.BIG -> 800f
            }
        }
    }

    private lateinit var world: World<Layer>

    private var elapsed = 0f
    private var duration = 0f
    private var finished = false
    private var onFinished: (() -> Unit)? = null

    private fun init(
        world: World<Layer>,
        kind: AttackEffectKind,
        centerX: Float,
        centerY: Float,
        onFinished: () -> Unit,
    ): AttackEffect {
        this.world = world
        this.onFinished = onFinished

        bitmap = gameContext.res.getBitmap(effectResId(kind))
        fps = EFFECT_FPS
        frameCount = effectFrameCount(kind)

        elapsed = 0f
        duration = frameCount / EFFECT_FPS
        finished = false

        val drawWidth = effectDrawWidth(kind)
        val drawHeight = drawWidth * effectFrameHeight(kind) / effectFrameWidth(kind)

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

            world.remove(this, Layer.ATTACK)
            callback?.invoke()
        }
    }

    override fun draw(canvas: Canvas) {
        if (finished) return

        syncDstRect()

        val frameIndex = ((elapsed * EFFECT_FPS).toInt())
            .coerceIn(0, frameCount - 1)

        srcRect?.set(
            frameIndex * frameWidth,
            0,
            (frameIndex + 1) * frameWidth,
            frameHeight,
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
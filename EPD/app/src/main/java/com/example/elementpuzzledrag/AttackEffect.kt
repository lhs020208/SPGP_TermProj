package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.AnimSprite
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

enum class AttackEffectKind {
    SMALL,
    NORMAL1,
    NORMAL2,
    BIG,
}

class AttackEffect(
    gctx: GameContext,
    private val world: World<Layer>,
    private val kind: AttackEffectKind,
    centerX: Float,
    centerY: Float,
) : AnimSprite(
    gctx = gctx,
    resId = effectResId(kind),
    fps = EFFECT_FPS,
    frameCount = effectFrameCount(kind),
) {
    companion object {
        private const val EFFECT_FPS = 30f

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

    private var elapsed = 0f
    private val duration = effectFrameCount(kind) / EFFECT_FPS

    init {
        val drawWidth = effectDrawWidth(kind)
        val drawHeight = drawWidth * effectFrameHeight(kind) / effectFrameWidth(kind)

        setSize(drawWidth, drawHeight)
        setCenter(centerX, centerY)
    }

    override fun update(gctx: GameContext) {
        elapsed += gctx.frameTime

        if (elapsed >= duration) {
            world.remove(this, Layer.ATTACK)
        }
    }
}
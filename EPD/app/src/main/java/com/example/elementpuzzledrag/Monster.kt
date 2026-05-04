package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IBoxCollidable

class Monster(
    private val gameContext: GameContext,
    val attribute: DropType,
    val attackPower: Int,
    hp: Int,
    val maxHp: Int = hp,
    val MaxremainingAttackTurns: Int,
    val hpGaugeSize: HpGaugeSize,
    resId: Int,
    centerX: Float,
    centerY: Float,
    drawWidth: Float? = null,
    drawHeight: Float? = null,
    val attackMotionDistance: Float = 30f,
    val attackMotionSpeed: Float = 375f,
) : Sprite(gameContext, resId), IBoxCollidable {

    enum class HpGaugeSize(
        val barWidth: Float,
        val barHeight: Float,
        val gap: Float,
    ) {
        SMALL(
            barWidth = 200f,
            barHeight = 11f,
            gap = 7f,
        ),
        MEDIUM(
            barWidth = 400f,
            barHeight = 18f,
            gap = 11f,
        ),
        LARGE(
            barWidth = 800f,
            barHeight = 28f,
            gap = 16f,
        ),
    }

    companion object {
        private const val COLLISION_INSET_RATIO = 0.05f

        private val HP_BAR_BG_COLOR = Color.rgb(0x50, 0x16, 0x4A)
        private const val WARNING_ATTACK_TURN_SCALE = 1.2f
        private const val ATTACK_BODY_MAX_SCALE = 1.2f
    }

    var hp: Int = hp.coerceIn(0, maxHp)
        private set
    var remainingAttackTurns = MaxremainingAttackTurns.coerceAtLeast(0)
    override val collisionRect = RectF()

    private enum class AttackMotionPhase {
        IDLE,
        DOWN,
        UP,
    }

    private var attackMotionPhase = AttackMotionPhase.IDLE
    private var bodyOffsetY = 0f
    private var bodyScale = 1f

    private var onAttackHit: (() -> Unit)? = null
    private var onAttackMotionFinished: (() -> Unit)? = null
    private val hpBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = HP_BAR_BG_COLOR
    }

    private val hpBarFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private fun attackTurnResId(): Int {
        return when (remainingAttackTurns.coerceIn(0, 10)) {
            0 -> R.mipmap.in0
            1 -> R.mipmap.in1
            2 -> R.mipmap.in2
            3 -> R.mipmap.in3
            4 -> R.mipmap.in4
            5 -> R.mipmap.in5
            6 -> R.mipmap.in6
            7 -> R.mipmap.in7
            8 -> R.mipmap.in8
            9 -> R.mipmap.in9
            else -> R.mipmap.in10
        }
    }

    init {
        require(attribute != DropType.HP) {
            "Monster attribute cannot be DropType.HP"
        }
        require(maxHp > 0) {
            "Monster maxHp must be positive"
        }

        val widthToUse = drawWidth ?: bitmapWidth.toFloat() * 3f
        val heightToUse = drawHeight ?: bitmapHeight.toFloat() * 3f

        setSize(widthToUse, heightToUse)
        setCenter(centerX, centerY)
        updateCollisionRect()
    }

    fun takeDamage(amount: Int) {
        if (amount <= 0) return
        hp = (hp - amount).coerceAtLeast(0)
    }

    fun heal(amount: Int) {
        if (amount <= 0) return
        hp = (hp + amount).coerceAtMost(maxHp)
    }

    fun decreaseAttackTurn() {
        if (remainingAttackTurns > 0) {
            remainingAttackTurns--
        }
    }

    fun resetAttackTurn(turns: Int) {
        remainingAttackTurns = turns.coerceAtLeast(0)
    }

    fun isDead(): Boolean {
        return hp <= 0
    }

    private fun updateCollisionRect() {
        collisionRect.set(dstRect)

        val inset = kotlin.math.min(width, height) * COLLISION_INSET_RATIO
        collisionRect.inset(inset, inset)
    }

    fun containsWorldPoint(px: Float, py: Float): Boolean {
        return collisionRect.contains(px, py)
    }

    fun startAttackMotion(
        onHit: () -> Unit,
        onFinished: () -> Unit,
    ): Boolean {
        if (attackMotionPhase != AttackMotionPhase.IDLE) return false

        bodyOffsetY = 0f
        bodyScale = 1f
        attackMotionPhase = AttackMotionPhase.DOWN

        onAttackHit = onHit
        onAttackMotionFinished = onFinished

        return true
    }

    fun getTargetMarkerCenter(markerHeight: Float, markerGap: Float): Pair<Float, Float> {
        val hpGaugeBottom = y + height / 2f + hpGaugeSize.gap + hpGaugeSize.barHeight
        val markerCenterX = x
        val markerCenterY = hpGaugeBottom + markerGap + markerHeight / 2f

        return markerCenterX to markerCenterY
    }

    private fun hpBarFgColor(): Int {
        return when (attribute) {
            DropType.FIRE -> Color.rgb(0xC0, 0x00, 0x00)
            DropType.WATER -> Color.rgb(0x00, 0x00, 0xFF)
            DropType.LEAF -> Color.rgb(0x92, 0xD0, 0x50)
            DropType.LIGHT -> Color.rgb(0xFF, 0xFF, 0x00)
            DropType.DARK -> Color.rgb(0x70, 0x30, 0xA0)
            DropType.HP -> Color.rgb(0xE5, 0x9E, 0xDD)
        }
    }

    private fun drawHpGauge(canvas: Canvas) {
        val barWidth = hpGaugeSize.barWidth
        val barHeight = hpGaugeSize.barHeight
        val gap = hpGaugeSize.gap

        val left = x - barWidth / 2f
        val top = y + height / 2f + gap
        val right = x + barWidth / 2f
        val bottom = top + barHeight

        val bgRect = RectF(left, top, right, bottom)
        canvas.drawRect(bgRect, hpBarBgPaint)

        val hpRatio = if (maxHp > 0) {
            hp.toFloat() / maxHp.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)

        if (hpRatio <= 0f) return

        val fgRect = RectF(
            left,
            top,
            left + barWidth * hpRatio,
            bottom,
        )
        hpBarFgPaint.color = hpBarFgColor()
        canvas.drawRect(fgRect, hpBarFgPaint)
    }

    private fun attackTurnIconScale(): Float {
        return if (remainingAttackTurns <= 1) {
            WARNING_ATTACK_TURN_SCALE
        } else {
            1f
        }
    }

    private fun drawAttackTurnIcon(canvas: Canvas) {
        val turnBitmap = gameContext.res.getBitmap(attackTurnResId())

        val iconWidth = turnBitmap.width
        val iconHeight = turnBitmap.height
        val gap = 40f

        val baseDst = RectF(
            x - iconWidth / 3f + 50f,
            y - height / 4f - (iconHeight + 40f) / 2f - gap,
            x + iconWidth / 3f + 50f,
            y - height / 4f - gap,
        )

        val scale = attackTurnIconScale()
        val centerX = baseDst.centerX()
        val centerY = baseDst.centerY()
        val scaledWidth = baseDst.width() * scale
        val scaledHeight = baseDst.height() * scale

        val dst = RectF(
            centerX - scaledWidth / 2f,
            centerY - scaledHeight / 2f,
            centerX + scaledWidth / 2f,
            centerY + scaledHeight / 2f,
        )

        canvas.drawBitmap(turnBitmap, null, dst, null)
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(0f, bodyOffsetY)
        canvas.scale(bodyScale, bodyScale, x, y + bodyOffsetY)
        super.draw(canvas)
        canvas.restore()

        drawHpGauge(canvas)
        drawAttackTurnIcon(canvas)
    }

    override fun update(gctx: GameContext) {
        when (attackMotionPhase) {
            AttackMotionPhase.IDLE -> return

            AttackMotionPhase.DOWN -> {
                bodyOffsetY += attackMotionSpeed * gctx.frameTime

                val progress = (bodyOffsetY / attackMotionDistance).coerceIn(0f, 1f)
                bodyScale = 1f + (ATTACK_BODY_MAX_SCALE - 1f) * progress

                if (bodyOffsetY >= attackMotionDistance) {
                    bodyOffsetY = attackMotionDistance
                    bodyScale = ATTACK_BODY_MAX_SCALE

                    val hitCallback = onAttackHit
                    onAttackHit = null
                    hitCallback?.invoke()

                    attackMotionPhase = AttackMotionPhase.UP
                }
            }

            AttackMotionPhase.UP -> {
                bodyOffsetY -= attackMotionSpeed * gctx.frameTime

                val progress = (bodyOffsetY / attackMotionDistance).coerceIn(0f, 1f)
                bodyScale = 1f + (ATTACK_BODY_MAX_SCALE - 1f) * progress

                if (bodyOffsetY <= 0f) {
                    bodyOffsetY = 0f
                    bodyScale = 1f
                    attackMotionPhase = AttackMotionPhase.IDLE

                    val finishedCallback = onAttackMotionFinished
                    onAttackMotionFinished = null
                    finishedCallback?.invoke()
                }
            }
        }
    }
}
package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

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
) : Sprite(gameContext, resId) {

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
        private val HP_BAR_FG_COLOR = Color.rgb(0xE5, 0x9E, 0xDD)
        private val HP_BAR_BG_COLOR = Color.rgb(0x50, 0x16, 0x4A)
    }

    var hp: Int = hp.coerceIn(0, maxHp)
        private set

    var remainingAttackTurns = MaxremainingAttackTurns.coerceAtLeast(0)

    private val hpBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = HP_BAR_BG_COLOR
    }

    private val hpBarFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = HP_BAR_FG_COLOR
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
        canvas.drawRect(fgRect, hpBarFgPaint)
    }

    private fun drawAttackTurnIcon(canvas: Canvas) {
        val turnBitmap = gameContext.res.getBitmap(attackTurnResId())

        val iconWidth = turnBitmap.width
        val iconHeight = turnBitmap.height
        val gap = 40f

        val dst = RectF(
            x - iconWidth / 3f + 50f,
            y - height / 4f - (iconHeight + 40f) / 2f - gap,
            x + iconWidth / 3f + 50f,
            y - height / 4f - gap,
        )

        canvas.drawBitmap(turnBitmap, null, dst, null)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawHpGauge(canvas)
        drawAttackTurnIcon(canvas)
    }
}
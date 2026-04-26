package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Monster(
    private val gameContext: GameContext,
    val attribute: DropType,
    val attackPower: Int,
    var hp: Int,
    var remainingAttackTurns: Int,
    resId: Int,
    centerX: Float,
    centerY: Float,
    drawWidth: Float? = null,
    drawHeight: Float? = null,
) : Sprite(gameContext, resId) {

    val maxHp: Int = hp
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

        val widthToUse = drawWidth ?: bitmapWidth.toFloat() * 3f
        val heightToUse = drawHeight ?: bitmapHeight.toFloat() * 3f

        setSize(widthToUse, heightToUse)
        setCenter(centerX, centerY)
    }

    fun takeDamage(amount: Int) {
        hp = (hp - amount).coerceAtLeast(0)
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

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

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
}
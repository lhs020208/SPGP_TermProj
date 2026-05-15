package com.example.elementpuzzledrag

data class StageSpec(
    val backgroundResId: Int,
    val monsters: List<MonsterSpec>,
)

data class MonsterSpec(
    val attribute: DropType,
    val attackPower: Int,
    val hp: Int,
    val maxRemainingAttackTurns: Int,
    val hpGaugeSize: Monster.HpGaugeSize,
    val resId: Int,
    val centerXOffsetFromStageCenter: Float,
    val centerYOffsetFromStageCenter: Float,
    val drawWidth: Float? = null,
    val drawHeight: Float? = null,
    val attackMotionDistance: Float? = null,
    val attackMotionSpeed: Float? = null,
)

object StageData {
    val stages = listOf(
        StageSpec(
            backgroundResId = R.mipmap.stage1,
            monsters = listOf(
                MonsterSpec(
                    attribute = DropType.FIRE,
                    attackPower = 1000,
                    hp = 500,
                    maxRemainingAttackTurns = 3,
                    hpGaugeSize = Monster.HpGaugeSize.SMALL,
                    resId = R.mipmap.m_firemonmus,
                    centerXOffsetFromStageCenter = 0f,
                    centerYOffsetFromStageCenter = 0f,
                ),
                MonsterSpec(
                    attribute = DropType.LEAF,
                    attackPower = 1000,
                    hp = 500,
                    maxRemainingAttackTurns = 3,
                    hpGaugeSize = Monster.HpGaugeSize.SMALL,
                    resId = R.mipmap.m_leafmonmus,
                    centerXOffsetFromStageCenter = -300f,
                    centerYOffsetFromStageCenter = 100f,
                ),
                MonsterSpec(
                    attribute = DropType.WATER,
                    attackPower = 1000,
                    hp = 500,
                    maxRemainingAttackTurns = 3,
                    hpGaugeSize = Monster.HpGaugeSize.SMALL,
                    resId = R.mipmap.m_watermonmus,
                    centerXOffsetFromStageCenter = 300f,
                    centerYOffsetFromStageCenter = 100f,
                ),
            ),
        ),
    )
}
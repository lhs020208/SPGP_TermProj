package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import android.view.MotionEvent
import kotlin.math.roundToInt
import kotlin.random.Random

class MainScene(gctx: GameContext) : Scene(gctx) {

    companion object {
        private const val SHOW_HIDDEN_DROPS_FOR_DEBUG = false
        private const val PLAYER_ATTACK_BASE_CONSTANT = 10f
        private const val DEFAULT_SKILL_MULTIPLIER = 1f
        private const val ATTACK_UP_MAX_COOLDOWN = 3
        private const val DROP_CHANGE_MAX_COOLDOWN = 5

        private val BASIC_PLAYER_ATTACK_ORDER = listOf(
            DropType.FIRE,
            DropType.WATER,
            DropType.LEAF,
            DropType.LIGHT,
            DropType.DARK,
        )
    }

    override val world = World(
        if (SHOW_HIDDEN_DROPS_FOR_DEBUG) {
            arrayOf(
                Layer.PUZZLE_BG,
                Layer.STAGE,
                Layer.HUD,
                Layer.BOARD,
                Layer.HOLDING,
                Layer.OVERLAY,
                Layer.MONSTER,
                Layer.SKILL_UI,
            )
        } else {
            arrayOf(
                Layer.PUZZLE_BG,
                Layer.BOARD,
                Layer.STAGE,
                Layer.HUD,
                Layer.HOLDING,
                Layer.OVERLAY,
                Layer.MONSTER,
                Layer.SKILL_UI,
            )
        }
    )

    private lateinit var board: Board

    private val monsters = mutableListOf<Monster>()
    private var targetedMonster: Monster? = null
    private var targetMarker: TargetMarker? = null
    private var attackTargetLocked = false

    private val elementSlots = mutableListOf<ElementSlot>()
    private val activeSkillIcons = mutableListOf<SkillIcon>()
    private var activeSkillElementType: DropType? = null

    private data class SkillKey(
        val elementType: DropType,
        val skillType: SkillType,
    )

    private val skillCooldowns = mutableMapOf<SkillKey, Int>()

    init {
        initSkillCooldowns()

        val screenW = gctx.metrics.width      // 900
        val screenH = gctx.metrics.height     // 1600
        val unitH = screenH / 16f             // 100

        val stageTop = 0f
        val stageHeight = 6.5f * unitH

        val elementTop = stageTop + stageHeight
        val elementHeight = 1.5f * unitH

        val hpTop = elementTop + elementHeight
        val hpHeight = 0.5f * unitH

        val puzzleTop = hpTop + hpHeight
        val puzzleHeight = 7.5f * unitH

        val typeGuideMargin = 20f
        val typeGuideWidth = 130f
        val typeGuideHeight = typeGuideWidth * 440f / 327f

        world.add(
            UiSprite(
                gctx,
                R.mipmap.stage1,
                0f,
                stageTop,
                screenW,
                stageHeight,
            ),
            Layer.STAGE,
        )

        val elementSlotWidth = screenW / 6f
        val elementSlotHeight = elementHeight

        addElementSlot(
            ElementSlot(
                gctx = gctx,
                elementType = DropType.FIRE,
                resId = R.mipmap.i_fire,
                slotLeft = 0f * elementSlotWidth,
                slotTop = elementTop,
                slotWidth = elementSlotWidth,
                slotHeight = elementSlotHeight,
            )
        )

        addElementSlot(
            ElementSlot(
                gctx = gctx,
                elementType = DropType.WATER,
                resId = R.mipmap.i_water,
                slotLeft = 1f * elementSlotWidth,
                slotTop = elementTop,
                slotWidth = elementSlotWidth,
                slotHeight = elementSlotHeight,
            )
        )

        addElementSlot(
            ElementSlot(
                gctx = gctx,
                elementType = DropType.LEAF,
                resId = R.mipmap.i_leaf,
                slotLeft = 2f * elementSlotWidth,
                slotTop = elementTop,
                slotWidth = elementSlotWidth,
                slotHeight = elementSlotHeight,
            )
        )

        addElementSlot(
            ElementSlot(
                gctx = gctx,
                elementType = DropType.LIGHT,
                resId = R.mipmap.i_light,
                slotLeft = 3f * elementSlotWidth,
                slotTop = elementTop,
                slotWidth = elementSlotWidth,
                slotHeight = elementSlotHeight,
            )
        )

        addElementSlot(
            ElementSlot(
                gctx = gctx,
                elementType = DropType.DARK,
                resId = R.mipmap.i_dark,
                slotLeft = 4f * elementSlotWidth,
                slotTop = elementTop,
                slotWidth = elementSlotWidth,
                slotHeight = elementSlotHeight,
            )
        )

        addElementSlot(
            ElementSlot(
                gctx = gctx,
                elementType = DropType.HP,
                resId = R.mipmap.i_hp,
                slotLeft = 5f * elementSlotWidth,
                slotTop = elementTop,
                slotWidth = elementSlotWidth,
                slotHeight = elementSlotHeight,
            )
        )

        world.add(
            UiSprite(
                gctx,
                R.mipmap.type,
                typeGuideMargin,
                typeGuideMargin,
                typeGuideWidth,
                typeGuideHeight,
            ),
            Layer.HUD,
        )

        world.add(
            HpBar(
                gctx,
                0f,
                hpTop,
                screenW,
                hpHeight,
                1f,
            ),
            Layer.HUD,
        )

        world.add(
            UiSprite(
                gctx,
                R.mipmap.i_puzzle_plane,
                0f,
                puzzleTop,
                screenW,
                puzzleHeight,
            ),
            Layer.PUZZLE_BG,
        )

        addMonster(
            Monster(
                gameContext = gctx,
                attribute = DropType.FIRE,
                attackPower = 100,
                hp = 500,
                MaxremainingAttackTurns = 3,
                hpGaugeSize = Monster.HpGaugeSize.SMALL,
                resId = R.mipmap.m_firemonmus,
                centerX = screenW / 2f,
                centerY = stageTop + stageHeight / 2f,
            )
        )

        addMonster(
            Monster(
                gameContext = gctx,
                attribute = DropType.LEAF,
                attackPower = 100,
                hp = 500,
                MaxremainingAttackTurns = 3,
                hpGaugeSize = Monster.HpGaugeSize.SMALL,
                resId = R.mipmap.m_leafmonmus,
                centerX = screenW / 2f - 300,
                centerY = stageTop + stageHeight / 2f + 100,
            )
        )

        addMonster(
            Monster(
                gameContext = gctx,
                attribute = DropType.WATER,
                attackPower = 100,
                hp = 500,
                MaxremainingAttackTurns = 3,
                hpGaugeSize = Monster.HpGaugeSize.SMALL,
                resId = R.mipmap.m_watermonmus,
                centerX = screenW / 2f + 300,
                centerY = stageTop + stageHeight / 2f + 100,
            )
        )

        board = Board(
            gctx = gctx,
            world = world,
            onPuzzleDragStarted = {
                clearSkillIcons()
                attackTargetLocked = true
            },
            onPuzzleTurnFinishedWithoutAttack = {
                decreaseAllSkillCooldowns()
                attackTargetLocked = false
            },
        )
        world.add(board, Layer.OVERLAY)
    }

    private fun addMonster(monster: Monster) {
        monsters.add(monster)
        world.add(monster, Layer.MONSTER)
    }

    private fun addElementSlot(slot: ElementSlot) {
        elementSlots.add(slot)
        world.add(slot, Layer.HUD)
    }

    private fun isSkillElementType(type: DropType): Boolean {
        return when (type) {
            DropType.FIRE,
            DropType.WATER,
            DropType.LEAF,
            DropType.LIGHT,
            DropType.DARK -> true

            DropType.HP -> false
        }
    }

    private fun initSkillCooldowns() {
        for (elementType in BASIC_PLAYER_ATTACK_ORDER) {
            for (skillType in SkillType.entries) {
                val key = SkillKey(elementType, skillType)
                skillCooldowns[key] = getSkillMaxCooldown(skillType)
            }
        }
    }

    private fun getSkillMaxCooldown(skillType: SkillType): Int {
        return when (skillType) {
            SkillType.ATTACK_UP -> ATTACK_UP_MAX_COOLDOWN
            SkillType.DROP_CHANGE -> DROP_CHANGE_MAX_COOLDOWN
        }
    }

    private fun getSkillCooldown(
        elementType: DropType,
        skillType: SkillType,
    ): Int {
        val key = SkillKey(elementType, skillType)
        return skillCooldowns[key] ?: getSkillMaxCooldown(skillType)
    }

    private fun isSkillReady(
        elementType: DropType,
        skillType: SkillType,
    ): Boolean {
        return getSkillCooldown(elementType, skillType) <= 0
    }

    private fun resetSkillCooldown(
        elementType: DropType,
        skillType: SkillType,
    ) {
        val key = SkillKey(elementType, skillType)
        skillCooldowns[key] = getSkillMaxCooldown(skillType)
    }

    private fun decreaseAllSkillCooldowns() {
        for ((key, cooldown) in skillCooldowns.toMap()) {
            if (cooldown > 0) {
                skillCooldowns[key] = cooldown - 1
            }
        }
    }

    private fun findTouchedElementSlot(screenX: Float, screenY: Float): ElementSlot? {
        val pt = gctx.metrics.fromScreen(screenX, screenY)

        for (slot in elementSlots.asReversed()) {
            if (slot.containsWorldPoint(pt.x, pt.y)) {
                return slot
            }
        }

        return null
    }

    private fun showSkillIconsFor(slot: ElementSlot) {
        clearSkillIcons()

        if (!isSkillElementType(slot.elementType)) return

        activeSkillElementType = slot.elementType

        val iconWidth = slot.slotWidth
        val iconHeight = slot.slotHeight
        val iconLeft = slot.slotLeft

        val attackUpTop = slot.slotTop - iconHeight * 2f
        val dropChangeTop = slot.slotTop - iconHeight

        val attackUpIcon = SkillIcon(
            gctx = gctx,
            skillType = SkillType.ATTACK_UP,
            resId = R.mipmap.skill_attackup,
            left = iconLeft,
            top = attackUpTop,
            width = iconWidth,
            height = iconHeight,
            cooldownRemaining = getSkillCooldown(slot.elementType, SkillType.ATTACK_UP),
        )

        val dropChangeIcon = SkillIcon(
            gctx = gctx,
            skillType = SkillType.DROP_CHANGE,
            resId = R.mipmap.skill_dropchange,
            left = iconLeft,
            top = dropChangeTop,
            width = iconWidth,
            height = iconHeight,
            cooldownRemaining = getSkillCooldown(slot.elementType, SkillType.DROP_CHANGE),
        )

        activeSkillIcons.add(attackUpIcon)
        activeSkillIcons.add(dropChangeIcon)

        world.add(attackUpIcon, Layer.SKILL_UI)
        world.add(dropChangeIcon, Layer.SKILL_UI)
    }

    private fun clearSkillIcons() {
        for (icon in activeSkillIcons.toList()) {
            world.remove(icon, Layer.SKILL_UI)
        }

        activeSkillIcons.clear()
        activeSkillElementType = null
    }

    private fun findTouchedSkillIcon(screenX: Float, screenY: Float): SkillIcon? {
        val pt = gctx.metrics.fromScreen(screenX, screenY)

        for (icon in activeSkillIcons.asReversed()) {
            if (icon.containsWorldPoint(pt.x, pt.y)) {
                return icon
            }
        }

        return null
    }

    private fun handleSkillMenuTouch(screenX: Float, screenY: Float): Boolean {
        val touchedSkillIcon = findTouchedSkillIcon(screenX, screenY)
        val elementType = activeSkillElementType

        if (touchedSkillIcon == null || elementType == null) {
            clearSkillIcons()
            return true
        }

        if (!isSkillReady(elementType, touchedSkillIcon.skillType)) {
            return true
        }

        when (touchedSkillIcon.skillType) {
            SkillType.ATTACK_UP -> {
                onAttackUpSkillTouched(elementType)
            }

            SkillType.DROP_CHANGE -> {
                onDropChangeSkillTouched(elementType)
            }
        }

        resetSkillCooldown(elementType, touchedSkillIcon.skillType)
        clearSkillIcons()
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAttackUpSkillTouched(elementType: DropType) {
        // TODO:
        // 나중에 이 속성의 공격력 상승 스킬을 적용한다.
        // 예: elementType 속성 공격의 skillMultiplier를 1.5f로 설정.
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDropChangeSkillTouched(elementType: DropType) {
        // TODO:
        // 나중에 이 속성으로 드롭 색상 변경 스킬을 적용한다.
    }

    private fun removeMonster(monster: Monster) {
        if (targetedMonster === monster) {
            clearAttackTarget()
        }

        monsters.remove(monster)
        world.remove(monster, Layer.MONSTER)
    }

    private fun getAttackTarget(): Monster? {
        return targetedMonster
    }

    private fun clearAttackTarget() {
        targetMarker?.let { marker ->
            world.remove(marker, Layer.MONSTER)
        }

        targetMarker = null
        targetedMonster = null
    }

    private fun clearCurrentStageMonsters() {
        clearAttackTarget()

        for (monster in monsters.toList()) {
            world.remove(monster, Layer.MONSTER)
        }

        monsters.clear()
    }

    private fun onStageCleared() {
        clearCurrentStageMonsters()
    }

    private fun setAttackTarget(monster: Monster) {
        clearAttackTarget()

        targetedMonster = monster

        val marker = TargetMarker(gctx, monster)
        targetMarker = marker
        world.add(marker, Layer.MONSTER)
    }

    private fun toggleAttackTarget(monster: Monster) {
        if (targetedMonster === monster) {
            clearAttackTarget()
        } else {
            setAttackTarget(monster)
        }
    }

    private fun findTouchedMonster(screenX: Float, screenY: Float): Monster? {
        val pt = gctx.metrics.fromScreen(screenX, screenY)

        for (monster in monsters.asReversed()) {
            if (monster.containsWorldPoint(pt.x, pt.y)) {
                return monster
            }
        }

        return null
    }



    private fun performPlayerAttacks(result: PlayerAttackResult) {
        if (result.chainCount <= 0) return
        if (monsters.isEmpty()) return

        val attackOrder = getPlayerAttackOrder()

        var finalStageTarget: Monster? = null

        for (attackAttribute in attackOrder) {
            if (finalStageTarget == null) {
                val aliveMonsters = getAliveMonsters()

                if (aliveMonsters.size == 1) {
                    finalStageTarget = aliveMonsters.first()
                }
            }

            val target = chooseAttackTargetFor(
                attackAttribute = attackAttribute,
                finalStageTarget = finalStageTarget,
            ) ?: break

            val removedDropCount = result.removedDropCounts[attackAttribute] ?: 0
            val damage = calculatePlayerDamage(
                attackAttribute = attackAttribute,
                target = target,
                removedDropCount = removedDropCount,
                chainCount = result.chainCount,
            )

            applyPlayerDamage(
                attackAttribute = attackAttribute,
                target = target,
                damage = damage,
            )
        }

        if (finalStageTarget?.isDead() == true) {
            onStageCleared()
        }
    }

    private fun getPlayerAttackOrder(): List<DropType> {
        val target = targetedMonster?.takeUnless { it.isDead() }
            ?: return BASIC_PLAYER_ATTACK_ORDER

        val bestAttribute = BASIC_PLAYER_ATTACK_ORDER.firstOrNull { attackAttribute ->
            getAttributeMultiplier(
                attackAttribute = attackAttribute,
                defenseAttribute = target.attribute,
            ) > 1f
        } ?: return BASIC_PLAYER_ATTACK_ORDER

        return listOf(bestAttribute) + BASIC_PLAYER_ATTACK_ORDER.filter { it != bestAttribute }
    }

    private fun chooseAttackTargetFor(
        attackAttribute: DropType,
        finalStageTarget: Monster?,
    ): Monster? {
        if (finalStageTarget != null) {
            return finalStageTarget
        }

        targetedMonster?.let { target ->
            if (!target.isDead()) {
                return target
            }

            clearAttackTarget()
        }

        return chooseRandomMonsterFor(attackAttribute)
    }

    private fun chooseRandomMonsterFor(attackAttribute: DropType): Monster? {
        val candidates = getAliveMonsters()
        if (candidates.isEmpty()) return null

        val randomIndex = Random.nextInt(candidates.size)
        return candidates[randomIndex]
    }

    private fun getAliveMonsters(): List<Monster> {
        return monsters.filter { !it.isDead() }
    }

    private fun calculatePlayerDamage(
        attackAttribute: DropType,
        target: Monster,
        removedDropCount: Int,
        chainCount: Int,
    ): Int {
        if (removedDropCount <= 0) return 0
        if (chainCount <= 0) return 0

        val rawDamage =
            removedDropCount *
                    chainCount *
                    PLAYER_ATTACK_BASE_CONSTANT *
                    getAttributeMultiplier(
                        attackAttribute = attackAttribute,
                        defenseAttribute = target.attribute,
                    ) *
                    getSkillMultiplier(attackAttribute)

        return rawDamage.roundToInt().coerceAtLeast(0)
    }

    private fun getAttributeMultiplier(
        attackAttribute: DropType,
        defenseAttribute: DropType,
    ): Float {
        if (isStrongAgainst(attackAttribute, defenseAttribute)) {
            return 2f
        }

        if (isWeakAgainst(attackAttribute, defenseAttribute)) {
            return 0.5f
        }

        return 1f
    }

    private fun isStrongAgainst(
        attackAttribute: DropType,
        defenseAttribute: DropType,
    ): Boolean {
        return when (attackAttribute) {
            DropType.FIRE -> defenseAttribute == DropType.LEAF
            DropType.WATER -> defenseAttribute == DropType.FIRE
            DropType.LEAF -> defenseAttribute == DropType.WATER
            DropType.LIGHT -> defenseAttribute == DropType.DARK
            DropType.DARK -> defenseAttribute == DropType.LIGHT
            DropType.HP -> false
        }
    }

    private fun isWeakAgainst(
        attackAttribute: DropType,
        defenseAttribute: DropType,
    ): Boolean {
        return isStrongAgainst(
            attackAttribute = defenseAttribute,
            defenseAttribute = attackAttribute,
        )
    }

    private fun getSkillMultiplier(attackAttribute: DropType): Float {
        // TODO:
        // 나중에 스킬이 추가되면 여기서 속성별 스킬 배율을 반환한다.
        // 예: 불 속성 강화 스킬 사용 중이면 DropType.FIRE일 때 1.5f 반환.
        return DEFAULT_SKILL_MULTIPLIER
    }

    private fun applyPlayerDamage(
        attackAttribute: DropType,
        target: Monster,
        damage: Int,
    ) {
        if (damage <= 0) return

        // TODO 8번 확장:
        // 나중에는 damage가 target.hp보다 크면 초과 대미지를 계산해서
        // 다른 랜덤 몬스터에게 이어서 적용하는 처리를 이 함수 안에 넣으면 된다.
        //
        // TODO 9번 확장:
        // 마지막 남은 몬스터인 경우에는 초과 대미지 분산 없이
        // 모든 공격을 그대로 받게 하는 예외도 여기서 처리하면 된다.

        target.takeDamage(damage)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        val attackResult = board.consumeAttackResult() ?: return

        try {
            performPlayerAttacks(attackResult)
        } finally {
            decreaseAllSkillCooldowns()
            attackTargetLocked = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (activeSkillIcons.isNotEmpty()) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                return handleSkillMenuTouch(event.x, event.y)
            }

            return true
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val touchedElementSlot = findTouchedElementSlot(event.x, event.y)

            if (touchedElementSlot != null) {
                if (!attackTargetLocked && isSkillElementType(touchedElementSlot.elementType)) {
                    showSkillIconsFor(touchedElementSlot)
                }
                return true
            }

            val touchedMonster = findTouchedMonster(event.x, event.y)

            if (touchedMonster != null) {
                if (!attackTargetLocked) {
                    toggleAttackTarget(touchedMonster)
                }
                return true
            }
        }

        return board.onTouchEvent(event)
    }
}
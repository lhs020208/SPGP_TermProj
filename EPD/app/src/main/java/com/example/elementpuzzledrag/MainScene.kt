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
        private const val PLAYER_HEAL_BASE_CONSTANT = 100f
        private const val DEFAULT_SKILL_MULTIPLIER = 1f
        private const val ATTACK_UP_SKILL_MULTIPLIER = 1.5f
        private const val RECOVER_UP_SKILL_MULTIPLIER = 1.5f

        private const val ATTACK_PROJECTILE_SIZE_RATIO = 0.72f
        private const val ATTACK_PROJECTILE_MIN_SPEED = 780f
        private const val ATTACK_PROJECTILE_MAX_SPEED = 900f

        private const val ATTACK_UP_MAX_COOLDOWN = 3
        private const val DROP_CHANGE_MAX_COOLDOWN = 5

        private const val ATTACK_UP_TEXT_RIGHT_MARGIN = 24f
        private const val ATTACK_UP_TEXT_TOP = 24f
        private const val ATTACK_UP_TEXT_LINE_HEIGHT = 48f

        private const val PLAYER_MAX_HP = 10000

        private const val DEFAULT_MONSTER_ATTACK_MOTION_DISTANCE = 30f
        private const val DEFAULT_MONSTER_ATTACK_MOTION_SPEED = 375f
        private const val MONSTER_ATTACK_DELAY_SECONDS = 0.5f

        private val BASIC_PLAYER_ATTACK_ORDER = listOf(
            DropType.FIRE,
            DropType.WATER,
            DropType.LEAF,
            DropType.LIGHT,
            DropType.DARK,
        )

        private val BASIC_SKILL_ELEMENT_ORDER = listOf(
            DropType.FIRE,
            DropType.WATER,
            DropType.LEAF,
            DropType.LIGHT,
            DropType.DARK,
            DropType.HP,
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
                Layer.ATTACK,
                Layer.ATTACK_TEXT,
                Layer.DIM_OVERLAY,
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
                Layer.ATTACK,
                Layer.ATTACK_TEXT,
                Layer.DIM_OVERLAY,
                Layer.SKILL_UI,
            )
        }
    )

    private lateinit var board: Board
    private lateinit var playerHpBar: HpBar

    private val monsters = mutableListOf<Monster>()
    private var targetedMonster: Monster? = null
    private var targetMarker: TargetMarker? = null
    private var attackTargetLocked = false

    private val elementSlots = mutableListOf<ElementSlot>()
    private val activeSkillIcons = mutableListOf<SkillIcon>()
    private var activeSkillElementType: DropType? = null

    private var dropChangeSourceElementType: DropType? = null
    private var dropChangeDimOverlay: ScreenDimOverlay? = null
    private val dropChangeSlotOverlays = mutableListOf<ElementSlot>()
    private val dropChangeTargetMarkers = mutableListOf<DropChangeTargetMarker>()

    private data class SkillKey(
        val elementType: DropType,
        val skillType: SkillType,
    )

    private val skillCooldowns = mutableMapOf<SkillKey, Int>()

    private val activeAttackUpElements = mutableSetOf<DropType>()
    private val attackUpBuffTexts = mutableListOf<AttackUpBuffText>()

    private var activeRecoverUp = false
    private var pendingPlayerHealAmount = 0

    private val stageSpecs = StageData.stages

    private var currentStageIndex = -1
    private var stageBackground: UiSprite? = null
    private var pendingStageAdvance = false

    private data class MainSceneLayout(
        val screenW: Float,
        val screenH: Float,
        val unitH: Float,
        val stageTop: Float,
        val stageHeight: Float,
        val elementTop: Float,
        val elementHeight: Float,
        val hpTop: Float,
        val hpHeight: Float,
        val puzzleTop: Float,
        val puzzleHeight: Float,
    )

    private fun makeLayout(): MainSceneLayout {
        val screenW = gctx.metrics.width
        val screenH = gctx.metrics.height
        val unitH = screenH / 16f

        val stageTop = 0f
        val stageHeight = 6.5f * unitH

        val elementTop = stageTop + stageHeight
        val elementHeight = 1.5f * unitH

        val hpTop = elementTop + elementHeight
        val hpHeight = 0.5f * unitH

        val puzzleTop = hpTop + hpHeight
        val puzzleHeight = 7.5f * unitH

        return MainSceneLayout(
            screenW = screenW,
            screenH = screenH,
            unitH = unitH,
            stageTop = stageTop,
            stageHeight = stageHeight,
            elementTop = elementTop,
            elementHeight = elementHeight,
            hpTop = hpTop,
            hpHeight = hpHeight,
            puzzleTop = puzzleTop,
            puzzleHeight = puzzleHeight,
        )
    }

    private data class PendingPlayerAttack(
        val attackAttribute: DropType,
        val target: Monster,
        val damage: Int,
        val effectKind: AttackEffectKind,
    )

    private val pendingAttackDamageByMonster = mutableMapOf<Monster, Int>()
    private var activeAttackProjectileCount = 0
    private var activeAttackEffectCount = 0
    private var playerAttackAnimating = false
    private var playerAttackDamageApplied = false
    private val monsterAttackQueue = ArrayDeque<Monster>()
    private var monsterAttackAnimating = false
    private var waitingMonsterAttackDelay = false
    private var monsterAttackDelayRemaining = 0f
    private var currentMonsterAttackDelaySeconds = MONSTER_ATTACK_DELAY_SECONDS

    init {
        initSkillCooldowns()

        val layout = makeLayout()

        val screenW = layout.screenW

        val elementTop = layout.elementTop
        val elementHeight = layout.elementHeight

        val hpTop = layout.hpTop
        val hpHeight = layout.hpHeight

        val puzzleTop = layout.puzzleTop
        val puzzleHeight = layout.puzzleHeight

        val typeGuideMargin = 20f
        val typeGuideWidth = 130f
        val typeGuideHeight = typeGuideWidth * 440f / 327f

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

        playerHpBar = HpBar(
            gctx = gctx,
            left = 0f,
            top = hpTop,
            barWidth = screenW,
            barHeight = hpHeight,
            maxHp = PLAYER_MAX_HP,
            currentHp = PLAYER_MAX_HP,
        )

        world.add(
            playerHpBar,
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

        loadStage(0)

        board = Board(
            gctx = gctx,
            world = world,
            onPuzzleDragStarted = {
                clearSkillIcons()
                clearDropChangeSelecting()
                attackTargetLocked = true
            },
            onPuzzleTurnFinishedWithoutAttack = {
                finishPlayerActionTurn()
            },
        )
        world.add(board, Layer.OVERLAY)
    }

    private fun attackProjectileResId(type: DropType): Int {
        return when (type) {
            DropType.FIRE -> R.mipmap.a_fire
            DropType.WATER -> R.mipmap.a_water
            DropType.LEAF -> R.mipmap.a_leaf
            DropType.LIGHT -> R.mipmap.a_light
            DropType.DARK -> R.mipmap.a_dark
            DropType.HP -> error("HP does not have an attack projectile")
        }
    }

    private fun loadStage(stageIndex: Int) {
        if (stageIndex !in stageSpecs.indices) {
            onAllStagesCleared()
            return
        }

        clearStageObjects()

        currentStageIndex = stageIndex

        val layout = makeLayout()
        val stage = stageSpecs[stageIndex]

        val background = UiSprite(
            gctx,
            stage.backgroundResId,
            0f,
            layout.stageTop,
            layout.screenW,
            layout.stageHeight,
        )

        stageBackground = background
        world.add(background, Layer.STAGE)

        for (monsterSpec in stage.monsters) {
            addMonster(createMonster(monsterSpec, layout))
        }
    }

    private fun createMonster(
        spec: MonsterSpec,
        layout: MainSceneLayout,
    ): Monster {
        val stageCenterX = layout.screenW / 2f
        val stageCenterY = layout.stageTop + layout.stageHeight / 2f

        return Monster(
            gameContext = gctx,
            attribute = spec.attribute,
            attackPower = spec.attackPower,
            hp = spec.hp,
            MaxremainingAttackTurns = spec.maxRemainingAttackTurns,
            hpGaugeSize = spec.hpGaugeSize,
            resId = spec.resId,
            centerX = stageCenterX + spec.centerXOffsetFromStageCenter,
            centerY = stageCenterY + spec.centerYOffsetFromStageCenter,
            drawWidth = spec.drawWidth,
            drawHeight = spec.drawHeight,
            attackMotionDistance = spec.attackMotionDistance
                ?: DEFAULT_MONSTER_ATTACK_MOTION_DISTANCE,
            attackMotionSpeed = spec.attackMotionSpeed
                ?: DEFAULT_MONSTER_ATTACK_MOTION_SPEED,
        )
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
        return type in BASIC_SKILL_ELEMENT_ORDER
    }

    private fun attackUpSkillIconResId(elementType: DropType): Int {
        return if (elementType == DropType.HP) {
            R.mipmap.skill_recoverup
        } else {
            R.mipmap.skill_attackup
        }
    }

    private fun attributeColor(type: DropType): Int {
        return when (type) {
            DropType.FIRE -> android.graphics.Color.rgb(0xC0, 0x00, 0x00)
            DropType.WATER -> android.graphics.Color.rgb(0x00, 0x00, 0xFF)
            DropType.LEAF -> android.graphics.Color.rgb(0x92, 0xD0, 0x50)
            DropType.LIGHT -> android.graphics.Color.rgb(0xFF, 0xFF, 0x00)
            DropType.DARK -> android.graphics.Color.rgb(0x70, 0x30, 0xA0)
            DropType.HP -> android.graphics.Color.rgb(0xE5, 0x9E, 0xDD)
        }
    }

    private fun getElementSlotCenter(type: DropType): Pair<Float, Float> {
        val slot = elementSlots.firstOrNull { it.elementType == type }
            ?: return gctx.metrics.width / 2f to 0f

        return slot.slotLeft + slot.slotWidth / 2f to
                slot.slotTop + slot.slotHeight / 2f
    }

    private fun getAttackProjectileSize(type: DropType): Float {
        val slot = elementSlots.firstOrNull { it.elementType == type }
            ?: return 100f

        return kotlin.math.min(slot.slotWidth, slot.slotHeight) * ATTACK_PROJECTILE_SIZE_RATIO
    }

    private fun randomAttackProjectileSpeed(): Float {
        return Random.nextFloat() *
                (ATTACK_PROJECTILE_MAX_SPEED - ATTACK_PROJECTILE_MIN_SPEED) +
                ATTACK_PROJECTILE_MIN_SPEED
    }

    private fun initSkillCooldowns() {
        for (elementType in BASIC_SKILL_ELEMENT_ORDER) {
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

    private fun isDropChangeSelecting(): Boolean {
        return dropChangeSourceElementType != null
    }

    private fun beginDropChangeSelecting(sourceElementType: DropType) {
        if (!isSkillElementType(sourceElementType)) return

        clearSkillIcons()
        clearDropChangeSelecting()

        dropChangeSourceElementType = sourceElementType

        val dimOverlay = ScreenDimOverlay(gctx)
        dropChangeDimOverlay = dimOverlay
        world.add(dimOverlay, Layer.DIM_OVERLAY)

        for (slot in elementSlots) {
            val slotOverlay = ElementSlot(
                gctx = gctx,
                elementType = slot.elementType,
                resId = slot.resId,
                slotLeft = slot.slotLeft,
                slotTop = slot.slotTop,
                slotWidth = slot.slotWidth,
                slotHeight = slot.slotHeight,
            )

            val marker = DropChangeTargetMarker(
                gctx = gctx,
                slot = slot,
            )

            dropChangeSlotOverlays.add(slotOverlay)
            dropChangeTargetMarkers.add(marker)

            world.add(slotOverlay, Layer.SKILL_UI)
            world.add(marker, Layer.SKILL_UI)
        }
    }

    private fun clearDropChangeSelecting() {
        dropChangeDimOverlay?.let { overlay ->
            world.remove(overlay, Layer.DIM_OVERLAY)
        }

        for (slotOverlay in dropChangeSlotOverlays.toList()) {
            world.remove(slotOverlay, Layer.SKILL_UI)
        }

        for (marker in dropChangeTargetMarkers.toList()) {
            world.remove(marker, Layer.SKILL_UI)
        }

        dropChangeDimOverlay = null
        dropChangeSlotOverlays.clear()
        dropChangeTargetMarkers.clear()
        dropChangeSourceElementType = null
    }

    private fun cancelDropChangeSelecting() {
        clearDropChangeSelecting()
    }

    private fun completeDropChangeSelecting(targetElementType: DropType) {
        val sourceElementType = dropChangeSourceElementType ?: return

        if (targetElementType == sourceElementType) {
            cancelDropChangeSelecting()
            return
        }

        board.changeVisibleDrops(
            fromType = targetElementType,
            toType = sourceElementType,
        )

        resetSkillCooldown(
            elementType = sourceElementType,
            skillType = SkillType.DROP_CHANGE,
        )

        clearDropChangeSelecting()
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
            resId = attackUpSkillIconResId(slot.elementType),
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

    private fun refreshAttackUpBuffTexts() {
        for (text in attackUpBuffTexts.toList()) {
            world.remove(text, Layer.SKILL_UI)
        }

        attackUpBuffTexts.clear()

        val right = gctx.metrics.width - ATTACK_UP_TEXT_RIGHT_MARGIN
        var index = 0

        for (elementType in BASIC_PLAYER_ATTACK_ORDER) {
            if (elementType !in activeAttackUpElements) continue

            val text = AttackUpBuffText(
                color = attributeColor(elementType),
                right = right,
                top = ATTACK_UP_TEXT_TOP + ATTACK_UP_TEXT_LINE_HEIGHT * index,
            )

            attackUpBuffTexts.add(text)
            world.add(text, Layer.SKILL_UI)

            index += 1
        }
    }

    private fun clearAttackUpBuffs() {
        activeAttackUpElements.clear()
        activeRecoverUp = false
        refreshAttackUpBuffTexts()
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
                resetSkillCooldown(elementType, touchedSkillIcon.skillType)
                clearSkillIcons()
            }

            SkillType.DROP_CHANGE -> {
                beginDropChangeSelecting(elementType)
            }
        }

        return true
    }

    private fun handleDropChangeSelectingTouch(
        screenX: Float,
        screenY: Float,
    ): Boolean {
        val touchedElementSlot = findTouchedElementSlot(screenX, screenY)

        if (touchedElementSlot == null) {
            cancelDropChangeSelecting()
            return true
        }

        val sourceElementType = dropChangeSourceElementType
        if (sourceElementType == null) {
            cancelDropChangeSelecting()
            return true
        }

        if (touchedElementSlot.elementType == sourceElementType) {
            cancelDropChangeSelecting()
            return true
        }

        completeDropChangeSelecting(touchedElementSlot.elementType)
        return true
    }

    private fun onAttackUpSkillTouched(elementType: DropType) {
        if (!isSkillElementType(elementType)) return

        if (elementType == DropType.HP) {
            activeRecoverUp = true
            return
        }

        activeAttackUpElements.add(elementType)
        refreshAttackUpBuffTexts()
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

    private fun clearStageMonsters() {
        clearAttackTarget()

        for (monster in monsters.toList()) {
            world.remove(monster, Layer.MONSTER)
        }

        monsters.clear()
    }

    private fun clearStageObjects() {
        clearStageMonsters()

        monsterAttackQueue.clear()
        monsterAttackAnimating = false
        waitingMonsterAttackDelay = false
        monsterAttackDelayRemaining = 0f
        currentMonsterAttackDelaySeconds = MONSTER_ATTACK_DELAY_SECONDS

        stageBackground?.let { background ->
            world.remove(background, Layer.STAGE)
        }
        stageBackground = null
    }

    private fun onStageCleared() {
        clearStageMonsters()
        pendingStageAdvance = true
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



    private fun planPlayerAttacks(result: PlayerAttackResult): List<PendingPlayerAttack> {
        if (result.chainCount <= 0) return emptyList()
        if (monsters.isEmpty()) return emptyList()

        val plannedHp = monsters.associateWith { it.hp }.toMutableMap()
        val attackOrder = getPlayerAttackOrder(plannedHp)

        val attacks = mutableListOf<PendingPlayerAttack>()
        var finalStageTarget: Monster? = null

        for (attackAttribute in attackOrder) {
            if (finalStageTarget == null) {
                val aliveMonsters = getAliveMonsters(plannedHp)

                if (aliveMonsters.size == 1) {
                    finalStageTarget = aliveMonsters.first()
                }
            }

            val target = chooseAttackTargetFor(
                attackAttribute = attackAttribute,
                finalStageTarget = finalStageTarget,
                plannedHp = plannedHp,
            ) ?: break

            val removedDropCount = result.removedDropCounts[attackAttribute] ?: 0
            val damage = calculatePlayerDamage(
                attackAttribute = attackAttribute,
                target = target,
                removedDropCount = removedDropCount,
                chainCount = result.chainCount,
            )

            if (damage > 0) {
                attacks.add(
                    PendingPlayerAttack(
                        attackAttribute = attackAttribute,
                        target = target,
                        damage = damage,
                        effectKind = getAttackEffectKind(
                            attackAttribute = attackAttribute,
                            target = target,
                        ),
                    )
                )
            }

            plannedHp[target] = (plannedHp[target] ?: target.hp) - damage
        }

        return attacks
    }

    private fun spawnAttackEffect(
        kind: AttackEffectKind,
        centerX: Float,
        centerY: Float,
    ) {
        activeAttackEffectCount += 1

        val effect = AttackEffect(
            gctx = gctx,
            world = world,
            kind = kind,
            centerX = centerX,
            centerY = centerY,
            onFinished = {
                onAttackEffectFinished()
            },
        )

        world.add(effect, Layer.ATTACK)
    }

    private fun onAttackEffectFinished() {
        activeAttackEffectCount -= 1

        if (activeAttackEffectCount < 0) {
            activeAttackEffectCount = 0
        }

        finishPlayerAttackTurnIfReady()
    }

    private fun getPlayerAttackOrder(
        plannedHp: Map<Monster, Int>,
    ): List<DropType> {
        val target = targetedMonster
            ?.takeIf { (plannedHp[it] ?: it.hp) > 0 }
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
        plannedHp: Map<Monster, Int>,
    ): Monster? {
        if (finalStageTarget != null) {
            return finalStageTarget
        }

        targetedMonster?.let { target ->
            if ((plannedHp[target] ?: target.hp) > 0) {
                return target
            }
        }

        return chooseRandomMonsterFor(
            attackAttribute = attackAttribute,
            plannedHp = plannedHp,
        )
    }

    private fun chooseRandomMonsterFor(
        attackAttribute: DropType,
        plannedHp: Map<Monster, Int>,
    ): Monster? {
        val candidates = getAliveMonsters(plannedHp)
        if (candidates.isEmpty()) return null

        val randomIndex = Random.nextInt(candidates.size)
        return candidates[randomIndex]
    }

    private fun getAliveMonsters(): List<Monster> {
        return monsters.filter { !it.isDead() }
    }

    private fun getAliveMonsters(
        plannedHp: Map<Monster, Int>,
    ): List<Monster> {
        return monsters.filter { (plannedHp[it] ?: it.hp) > 0 }
    }

    private fun startPlayerAttackAnimations(
        attacks: List<PendingPlayerAttack>,
        healAmount: Int,
    ) {
        pendingAttackDamageByMonster.clear()
        pendingPlayerHealAmount = healAmount

        if (healAmount > 0) {
            showPlayerHealText(healAmount)
        }

        activeAttackProjectileCount = 0
        activeAttackEffectCount = 0
        playerAttackDamageApplied = false

        if (attacks.isEmpty()) {
            applyPendingPlayerAttackDamage()
            finishPlayerActionTurn()
            return
        }

        playerAttackAnimating = true
        activeAttackProjectileCount = attacks.size

        for (attack in attacks) {
            pendingAttackDamageByMonster[attack.target] =
                (pendingAttackDamageByMonster[attack.target] ?: 0) + attack.damage

            val (startX, startY) = getElementSlotCenter(attack.attackAttribute)
            val projectileSize = getAttackProjectileSize(attack.attackAttribute)

            val targetX = attack.target.x
            val targetY = attack.target.y

            val projectile = AttackProjectile(
                gctx = gctx,
                world = world,
                resId = attackProjectileResId(attack.attackAttribute),
                startX = startX,
                startY = startY,
                targetX = targetX,
                targetY = targetY,
                size = projectileSize,
                speed = randomAttackProjectileSpeed(),
                onArrived = {
                    spawnAttackEffect(
                        kind = attack.effectKind,
                        centerX = targetX,
                        centerY = targetY,
                    )

                    showPlayerAttackDamageText(
                        attack = attack,
                        centerX = targetX,
                        centerY = targetY,
                    )

                    onAttackProjectileArrived()
                },
            )

            world.add(projectile, Layer.ATTACK)
        }
    }

    private fun onAttackProjectileArrived() {
        activeAttackProjectileCount -= 1

        if (activeAttackProjectileCount < 0) {
            activeAttackProjectileCount = 0
        }

        if (activeAttackProjectileCount <= 0) {
            applyPendingPlayerAttackDamage()
            finishPlayerAttackTurnIfReady()
        }
    }

    private fun applyPendingPlayerAttackDamage() {
        if (playerAttackDamageApplied) return

        if (pendingPlayerHealAmount > 0) {
            healPlayer(pendingPlayerHealAmount)
        }

        for ((monster, damage) in pendingAttackDamageByMonster.toMap()) {
            if (damage > 0) {
                monster.takeDamage(damage)
            }
        }

        pendingPlayerHealAmount = 0
        pendingAttackDamageByMonster.clear()
        playerAttackDamageApplied = true

        processDeadMonstersAfterPlayerAttack()
    }

    private fun finishPlayerAttackTurnIfReady() {
        if (!playerAttackAnimating) return
        if (!playerAttackDamageApplied) return
        if (activeAttackProjectileCount > 0) return
        if (activeAttackEffectCount > 0) return

        playerAttackAnimating = false
        finishPlayerActionTurn()
    }

    private fun finishPlayerActionTurn() {
        decreaseAllSkillCooldowns()
        clearAttackUpBuffs()

        processMonsterTurnsAfterPlayerAction()
    }

    private fun processDeadMonstersAfterPlayerAttack() {
        if (monsters.isEmpty()) return

        val deadMonsters = monsters.filter { it.isDead() }
        if (deadMonsters.isEmpty()) return

        val aliveMonsters = monsters.filter { !it.isDead() }

        if (aliveMonsters.isEmpty()) {
            onStageCleared()
            return
        }

        for (monster in deadMonsters) {
            removeMonster(monster)
        }
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

    private fun calculatePlayerHealAmount(result: PlayerAttackResult): Int {
        if (result.chainCount <= 0) return 0

        val removedHpDropCount = result.removedDropCounts[DropType.HP] ?: 0
        if (removedHpDropCount <= 0) return 0

        val rawHeal =
            removedHpDropCount *
                    result.chainCount *
                    PLAYER_HEAL_BASE_CONSTANT *
                    getRecoverSkillMultiplier()

        return rawHeal.roundToInt().coerceAtLeast(0)
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

    private fun getAttackEffectKind(
        attackAttribute: DropType,
        target: Monster,
    ): AttackEffectKind {
        val multiplier = getAttributeMultiplier(
            attackAttribute = attackAttribute,
            defenseAttribute = target.attribute,
        )

        return when {
            multiplier < 1f -> AttackEffectKind.SMALL
            multiplier > 1f -> AttackEffectKind.BIG
            else -> {
                if (Random.nextBoolean()) {
                    AttackEffectKind.NORMAL1
                } else {
                    AttackEffectKind.NORMAL2
                }
            }
        }
    }

    private fun getPlayerAttackDamageTextScale(
        attackAttribute: DropType,
        target: Monster,
    ): Float {
        val multiplier = getAttributeMultiplier(
            attackAttribute = attackAttribute,
            defenseAttribute = target.attribute,
        )

        return when {
            multiplier > 1f -> 1.75f
            multiplier < 1f -> 1f
            else -> 1.5f
        }
    }

    private fun showPlayerAttackDamageText(
        attack: PendingPlayerAttack,
        centerX: Float,
        centerY: Float,
    ) {
        if (attack.damage <= 0) return

        val damageText = PlayerAttackDamageText(
            world = world,
            damage = attack.damage,
            color = attributeColor(attack.attackAttribute),
            centerX = centerX,
            centerY = centerY,
            sizeScale = getPlayerAttackDamageTextScale(
                attackAttribute = attack.attackAttribute,
                target = attack.target,
            ),
        )

        world.add(damageText, Layer.ATTACK_TEXT)
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
        return if (attackAttribute in activeAttackUpElements) {
            ATTACK_UP_SKILL_MULTIPLIER
        } else {
            DEFAULT_SKILL_MULTIPLIER
        }
    }

    private fun getRecoverSkillMultiplier(): Float {
        return if (activeRecoverUp) {
            RECOVER_UP_SKILL_MULTIPLIER
        } else {
            DEFAULT_SKILL_MULTIPLIER
        }
    }

    private fun applyPlayerDamage(
        attackAttribute: DropType,
        target: Monster,
        damage: Int,
    ) {
        if (damage <= 0) return
        target.takeDamage(damage)
    }

    private fun showPlayerDamageText(damage: Int) {
        if (damage <= 0) return

        val (centerX, centerY) = playerHpBar.getCurrentHpRightCenter()

        val damageText = PlayerDamageText(
            gctx = gctx,
            world = world,
            damage = damage,
            centerX = centerX,
            centerY = centerY,
        )

        world.add(damageText, Layer.HUD)
    }

    private fun showPlayerHealText(healAmount: Int) {
        if (healAmount <= 0) return

        val (centerX, centerY) = playerHpBar.getCurrentHpRightCenter()

        val healText = PlayerHealText(
            gctx = gctx,
            world = world,
            healAmount = healAmount,
            centerX = centerX,
            centerY = centerY,
        )

        world.add(healText, Layer.HUD)
    }

    private fun damagePlayer(amount: Int) {
        if (amount <= 0) return

        playerHpBar.takeDamage(amount)

        if (playerHpBar.currentHp <= 0) {
            playerHpBar.setHp(0)
        }

        showPlayerDamageText(amount)

        if (playerHpBar.currentHp <= 0) {
            onPlayerDead()
        }
    }

    private fun healPlayer(amount: Int) {
        playerHpBar.heal(amount)
    }

    private fun onPlayerDead() {
        // TODO:
        // 게임오버 구현 시 여기서 처리한다.
    }

    private fun willPlayerDieFromQueuedMonsterAttacks(): Boolean {
        val totalDamage = monsterAttackQueue.sumOf { monster ->
            if (monster.isDead() || monster !in monsters) {
                0
            } else {
                monster.attackPower
            }
        }

        return totalDamage >= playerHpBar.currentHp
    }

    private fun processMonsterTurnsAfterPlayerAction() {
        monsterAttackQueue.clear()

        if (playerHpBar.currentHp <= 0) {
            finishMonsterActionPhase()
            return
        }

        if (monsters.isEmpty()) {
            finishMonsterActionPhase()
            return
        }

        for (monster in monsters.toList()) {
            if (monster.isDead()) continue

            monster.decreaseAttackTurn()

            if (monster.remainingAttackTurns <= 0) {
                monsterAttackQueue.addLast(monster)
            }
        }

        currentMonsterAttackDelaySeconds = if (willPlayerDieFromQueuedMonsterAttacks()) {
            0f
        } else {
            MONSTER_ATTACK_DELAY_SECONDS
        }

        startNextMonsterAttackOrFinish()
    }

    private fun startNextMonsterAttackOrFinish() {
        if (playerHpBar.currentHp <= 0) {
            monsterAttackQueue.clear()
            finishMonsterActionPhase()
            return
        }

        if (monsterAttackQueue.isEmpty()) {
            finishMonsterActionPhase()
            return
        }

        if (currentMonsterAttackDelaySeconds > 0f) {
            waitingMonsterAttackDelay = true
            monsterAttackDelayRemaining = currentMonsterAttackDelaySeconds
            return
        }

        startNextMonsterAttackNow()
    }

    private fun startNextMonsterAttackNow() {
        if (playerHpBar.currentHp <= 0) {
            monsterAttackQueue.clear()
            finishMonsterActionPhase()
            return
        }

        if (monsterAttackQueue.isEmpty()) {
            finishMonsterActionPhase()
            return
        }

        val monster = monsterAttackQueue.removeFirst()

        if (monster.isDead() || monster !in monsters) {
            startNextMonsterAttackOrFinish()
            return
        }

        monsterAttackAnimating = true

        val started = monster.startAttackMotion(
            onHit = {
                monsterAttackPlayer(monster)
            },
            onFinished = {
                monster.resetAttackTurn(monster.MaxremainingAttackTurns)
                monsterAttackAnimating = false
                startNextMonsterAttackOrFinish()
            },
        )

        if (!started) {
            monsterAttackPlayer(monster)
            monster.resetAttackTurn(monster.MaxremainingAttackTurns)
            monsterAttackAnimating = false
            startNextMonsterAttackOrFinish()
        }
    }

    private fun updateMonsterAttackDelay(frameTime: Float) {
        if (!waitingMonsterAttackDelay) return

        monsterAttackDelayRemaining -= frameTime

        if (monsterAttackDelayRemaining > 0f) return

        waitingMonsterAttackDelay = false
        monsterAttackDelayRemaining = 0f

        startNextMonsterAttackNow()
    }

    private fun monsterAttackPlayer(monster: Monster) {
        damagePlayer(monster.attackPower)
    }

    private fun loadNextStageOrFinish() {
        val nextStageIndex = currentStageIndex + 1

        if (nextStageIndex in stageSpecs.indices) {
            loadStage(nextStageIndex)
        } else {
            onAllStagesCleared()
        }
    }

    private fun onAllStagesCleared() {
        // TODO:
        // 모든 스테이지 클리어 처리.
        // 지금은 1스테이지만 있으므로 배경은 그대로 두고 몬스터 없는 상태로 둔다.
    }

    private fun finishMonsterActionPhase() {
        monsterAttackAnimating = false
        waitingMonsterAttackDelay = false
        monsterAttackDelayRemaining = 0f
        currentMonsterAttackDelaySeconds = MONSTER_ATTACK_DELAY_SECONDS
        attackTargetLocked = false

        if (pendingStageAdvance) {
            pendingStageAdvance = false
            loadNextStageOrFinish()
        }
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        if (waitingMonsterAttackDelay) {
            updateMonsterAttackDelay(gctx.frameTime)
            return
        }

        if (playerAttackAnimating || monsterAttackAnimating || monsterAttackQueue.isNotEmpty()) {
            return
        }

        val attackResult = board.consumeAttackResult() ?: return

        val attacks = planPlayerAttacks(attackResult)
        val healAmount = calculatePlayerHealAmount(attackResult)

        startPlayerAttackAnimations(
            attacks = attacks,
            healAmount = healAmount,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (
            playerAttackAnimating ||
            monsterAttackAnimating ||
            waitingMonsterAttackDelay ||
            monsterAttackQueue.isNotEmpty()
        ) {
            return true
        }
        if (isDropChangeSelecting()) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                return handleDropChangeSelectingTouch(event.x, event.y)
            }

            return true
        }

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
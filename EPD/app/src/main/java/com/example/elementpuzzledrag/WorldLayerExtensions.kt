package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World

fun World<Layer>.moveBetweenLayers(
    obj: IGameObject,
    fromLayer: Layer,
    toLayer: Layer,
) {
    remove(obj, fromLayer)
    add(obj, toLayer)
}
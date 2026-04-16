package com.example.elementpuzzledrag

fun DropType.toResId(): Int {
    return when (this) {
        DropType.FIRE -> R.mipmap.d_fire
        DropType.WATER -> R.mipmap.d_water
        DropType.LEAF -> R.mipmap.d_leaf
        DropType.LIGHT -> R.mipmap.d_light
        DropType.DARK -> R.mipmap.d_dark
        DropType.HP -> R.mipmap.d_hp
    }
}
package de.hanno.ecs

fun Long.get(index: Int): Boolean = (this or (1L shl index)) > 1L

operator fun Long.get(index: Int, value: Boolean): Long = if(value) {
    this or (1L shl index)
} else {
    this and (1L shl index)
}

fun Long.withClearedComponents() = this shr generationShiftBitCount shl generationShiftBitCount
package de.hanno.ecs

operator fun Long.get(index: Int): Boolean = this and (1L shl index) != 0L

operator fun Long.get(index: Int, value: Boolean): Long = if(value) {
    this or (1L shl index)
} else {
    this and (1L shl index)
}

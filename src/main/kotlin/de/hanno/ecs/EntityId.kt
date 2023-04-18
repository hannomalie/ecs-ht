package de.hanno.ecs

internal val idShiftBitCount = 32
internal val generationShiftBitCount = 16

typealias EntityId = Long
fun EntityId(int: Int): Long = (int.toLong() shl idShiftBitCount) or 1L
val EntityId.idPart: Long get() = this shr idShiftBitCount
fun EntityId.toBinaryString(): String = toString(2)

fun ComponentId(int: Int): Long = (int.toLong() shl idShiftBitCount) or 2L

val Long.isEntity: Boolean get() = this[0]
val Long.isComponent: Boolean get() = this[1] && !this[0]
val Long.isInstanceOf: Boolean get() = this[1] && this[1]
val Long.targetInstance: Long get() = (idPart shl idShiftBitCount ) or 1L

context(World)
fun EntityId.getBinaryStrings(): String {
    return "       " + toBinaryString() + "\n" +
            "id   : " + idPart.shortBinaryString + "\n"
}

val Long.binaryString get() = toString(2).padStart(Long.SIZE_BITS, '0')
val Long.shortBinaryString get() = toString(2).padStart(Int.SIZE_BITS, '0')

context(World)
fun <T> EntityId.has(archetype: Archetype<T>): Boolean = entityIndex[this]?.contains(archetype.id) ?: false

context(World)
fun <T> EntityId.get(clazz: Class<T>): T? {
    val archeType = archetypes.first { it.correspondsTo(clazz) }

    val resolvedComponent = archeType.getFor(this) as T?

    return if(resolvedComponent != null) {
        resolvedComponent
    } else {
        val potentialInstanceOfs = entityIndex[this]?.filter { it.isInstanceOf } ?: emptyList()
        val potentialComponents = potentialInstanceOfs.mapNotNull { archeType.getFor(it.targetInstance) as T? }
        potentialComponents.firstOrNull()
    }
}

context(World)
inline fun <reified T> EntityId.on(noinline block: T.() -> Unit) {
    on(T::class.java, block)
}

context(World)
fun <T> EntityId.on(clazz: Class<T>, block: T.() -> Unit) {
    val archeType = archetypes.filterIsInstance<PackedArchetype<T>>().firstOrNull { it.correspondsTo(clazz) }

    archeType?.on(this, block)
}

context(World)
inline fun <reified T> EntityId.get(): T? = get(T::class.java)

context(World)
fun <T> EntityId.add(archetype: Archetype<T>) {
    entityIndex[this]!!.add(archetype.id)
    archetype.createFor(this)
}

context(World)
fun EntityId.delete() {
    if (isAlive) {
        entityIndex.remove(this)
        idsToRecycle.add(this) // TODO: Increment generation here
    }
}

context(World)
fun EntityId.remove(componentType: Archetype<*>) {
    entityIndex[this]!!.remove(componentType.id)
}

context(World)
val EntityId.isAlive: Boolean
    get() {
        val expectedGeneration = potentiallyOutdatedGeneration
        val actualGeneration = generation

        return expectedGeneration == actualGeneration
    }
context(World)
val EntityId.generation
    get() = this shr generationShiftBitCount

private val EntityId.potentiallyOutdatedGeneration get() = this shr generationShiftBitCount
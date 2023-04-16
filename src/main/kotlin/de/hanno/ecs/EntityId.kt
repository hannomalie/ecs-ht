package de.hanno.ecs

internal val idShiftBitCount = 32
internal val generationShiftBitCount = 16


class World(
    val maxEntityCount: Int = 100000,
) {
    internal val archetypes = mutableListOf<Archetype<*>>()
    internal val entityIndex = mutableMapOf<EntityId, MutableList<Archetype<*>>>()

    internal val idsToRecycle = mutableListOf<Long>()

    internal var entityCounter = 0L

    val entityCount: Int get() = entityCounter.toInt() - idsToRecycle.size

    private fun allocateId(): Long = idsToRecycle.firstOrNull()?.apply {
        idsToRecycle.remove(this)
    } ?: entityCounter++ shl idShiftBitCount

    fun Entity(): Long = allocateId().apply {
        entityIndex[this] = mutableListOf()
    }

    fun getEntity(id: EntityId) = if (entityIndex.containsKey(id)) id else null
}

typealias EntityId = Long
fun EntityId(int: Int): Long = int.toLong() shl idShiftBitCount
val EntityId.idPart: Long get() = this shr idShiftBitCount
fun EntityId.toBinaryString(): String = toString(2)

context(World)
fun EntityId.getBinaryStrings(): String {
    return "       " + toBinaryString() + "\n" +
            "id   : " + idPart.shortBinaryString + "\n"
}

val Long.binaryString get() = toString(2).padStart(Long.SIZE_BITS, '0')
val Long.shortBinaryString get() = toString(2).padStart(Int.SIZE_BITS, '0')

context(World)
fun <T> EntityId.has(archetype: Archetype<T>): Boolean = entityIndex[this]?.contains(archetype) ?: false

context(World)
fun <T> EntityId.get(clazz: Class<T>): T? {
    val archeType = archetypes.firstOrNull { it.correspondsTo(clazz) }

    return archeType?.getFor(this) as T?
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
    entityIndex[this]!!.add(archetype)
    archetype.createFor(this)
}

context(World)
fun EntityId.delete() {
    if (isAlive) {
        val actualGeneration = this shr generationShiftBitCount
        entityIndex.remove(this)
        idsToRecycle.add(actualGeneration.withClearedComponents())
    }
}

context(World)
fun EntityId.remove(componentType: Archetype<*>) {
    entityIndex[this]!!.remove(componentType)
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
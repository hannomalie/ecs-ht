package de.hanno.ecs

internal val idShiftBitCount = 32
internal val generationShiftBitCount = 16

class World(
    val maxEntityCount: Int = 100000,
    val archeTypes: List<Archetype<*>> = mutableListOf()
) {
    internal val entityIndex = LongArray(maxEntityCount)
    internal val idsToRecycle = mutableListOf<Long>()

    internal var entityCounter = 0L

    val entityCount: Int get() = entityCounter.toInt() - idsToRecycle.size

    private fun allocateId(): Long {
        return idsToRecycle.firstOrNull()?.apply { idsToRecycle.remove(this) } ?: entityCounter++ shl idShiftBitCount
    }

    fun Entity() = EntityId(allocateId())

    fun getEntity(id: Int): EntityId? {
        if(id > entityCounter) return null

        val entity = EntityId(EntityId(id).retrieveLatestEntity())

        return if(entity.isAlive) entity else null
    }
}

@JvmInline
value class EntityId(val id: Long)  {

    constructor(fromInt: Int): this(fromInt.toLong() shl idShiftBitCount )
    val idPart: Long get() = id shr idShiftBitCount

    override fun toString(): String = id.toString(2)
}

context(World)
fun Entity.retrieveLatestEntity() = entityIndex[idPart.toInt()]

context(World)
fun EntityId.getBinaryStrings(): String {
    return  "       " + id.binaryString + "\n" +
            "id   : " + idPart.shortBinaryString + "\n" +
            "comp : " + componentPart.shortBinaryString + "\n"
}

context(World)
val Entity.componentPart: Long get() = retrieveLatestEntity() shl idShiftBitCount shr idShiftBitCount

val Long.binaryString get() = toString(2).padStart(Long.SIZE_BITS, '0')
val Long.shortBinaryString get() = toString(2).padStart(Int.SIZE_BITS, '0')

typealias Entity = EntityId

context(World)
fun <T: Component> Entity.has(archetype: Archetype<T>): Boolean {
    val latestEntity = retrieveLatestEntity()
    val shiftedToComponentType = latestEntity shr archetype.index
    val onlyHighestBit = shiftedToComponentType and 1

    return onlyHighestBit == 1L
}

context(World)
fun <T: Component> Entity.get(clazz: Class<T>): T? {
    val latestEntity = retrieveLatestEntity()
    val archeType = archeTypes.filterIsInstance<ArchetypeImpl<T>>().firstOrNull { it.correspondsTo(clazz) }

    return archeType?.getFor(EntityId(latestEntity))
}

context(World)
inline fun <reified T: PackedComponent> Entity.on(noinline block: T.() -> Unit) {
    on(T::class.java, block)
}
context(World)
fun <T: PackedComponent> Entity.on(clazz: Class<T>, block: T.() -> Unit) {
    val latestEntity = retrieveLatestEntity()
    val archeType = archeTypes.filterIsInstance<PackedArchetype<T>>().firstOrNull { it.correspondsTo(clazz) }

    archeType?.on(EntityId(latestEntity), block)
}

context(World)
inline fun <reified T: PackedComponent> Entity.get(): T? = get(T::class.java)

context(World)
fun <T: PackedComponent> Entity.get(clazz: Class<T>): T? {
    val latestEntity = retrieveLatestEntity()
    val archeType = archeTypes.filterIsInstance<PackedArchetype<T>>().firstOrNull { it.correspondsTo(clazz) }

    return archeType?.getFor(EntityId(latestEntity))
}

context(World)
fun <T: BaseComponent> Entity.add(archetype: Archetype<T>) {
    entityIndex[idPart.toInt()] = id[archetype.index, true]

    archetype.createFor(this)
}

context(World)
fun Entity.delete() {
    if(isAlive) {
        val actualGeneration = retrieveLatestEntity() shr generationShiftBitCount
        val nextGeneration = actualGeneration + 1
        entityIndex[idPart.toInt()] = (nextGeneration or componentPart) shl generationShiftBitCount
        idsToRecycle.add(actualGeneration.withClearedComponents())
    }
}

context(World)
fun Entity.remove(componentType: Archetype<out Component>) {
    entityIndex[idPart.toInt()] = id[componentType.index, false]
}

context(World)
val Entity.isAlive: Boolean get() {
    val expectedGeneration = potentiallyOutdatedGeneration
    val actualGeneration = generation

    return expectedGeneration == actualGeneration
}
context(World)
val Entity.generation get() = retrieveLatestEntity() shr generationShiftBitCount

private val Entity.potentiallyOutdatedGeneration get() = id shr generationShiftBitCount
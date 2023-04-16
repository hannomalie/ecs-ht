package de.hanno.ecs

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
    } ?: (entityCounter++ shl idShiftBitCount)

    fun Entity(): Long = allocateId().apply {
        entityIndex[this] = mutableListOf()
    }

    fun getEntity(id: EntityId) = if (entityIndex.containsKey(id)) id else null
}
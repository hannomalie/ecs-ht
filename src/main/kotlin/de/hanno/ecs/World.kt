package de.hanno.ecs

class World(
    val maxEntityCount: Int = 100000,
) {
    internal val archetypes = mutableListOf<Archetype>()
    internal val registeredComponents = mutableMapOf<Class<*>, Long>()
    internal val entityIndex = mutableMapOf<EntityId, MutableList<Long>>()

    internal val idsToRecycle = mutableListOf<Long>()

    internal var entityCounter = 0L

    val entityCount: Int get() = entityIndex.size - idsToRecycle.size

    inline fun <reified T> register() = register(T::class.java)

    fun register(clazz: Class<*>): Long = ComponentId().apply {
        registeredComponents[clazz] = this
    }

    private fun allocateId(): Long = idsToRecycle.firstOrNull()?.apply {
        idsToRecycle.remove(this)
    } ?: (entityCounter++ shl idShiftBitCount)

    fun Entity(): Long {
        val allocatedId = allocateId().toEntityId()

        return allocatedId.apply {
            entityIndex[this] = mutableListOf()
        }
    }
    fun ComponentId(): Long {
        val allocatedId = (entityCounter++ shl idShiftBitCount).toComponentId()

        return allocatedId.apply {
            entityIndex[this] = mutableListOf()
        }
    }

    fun getEntity(id: EntityId) = if (entityIndex.containsKey(id)) id else null

    fun EntityId.setInstanceOf(other: EntityId) {
        entityIndex[this]!!.add(other.toInstanceOfIdentifier())
    }

    private fun EntityId.toInstanceOfIdentifier() = this or 3L
    private fun Long.toEntityId() = this or 1L
    private fun Long.toComponentId() = this or 2L
}
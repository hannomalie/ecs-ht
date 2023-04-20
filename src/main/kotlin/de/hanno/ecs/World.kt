package de.hanno.ecs

import java.lang.reflect.Constructor

class World(
    val maxEntityCount: Int = 100000,
) {
    internal val archetypes = mutableMapOf<Set<Long>, Archetype>()
    internal val registeredComponents = mutableMapOf<Class<*>, Long>()
    internal val factories = mutableMapOf<Class<*>, Constructor<*>>()
    internal val entityIndex = mutableMapOf<Long, MutableSet<Long>>()

    internal val idsToRecycle = mutableListOf<Long>()

    internal var entityCounter = 0L

    val entityCount: Int get() = entityIndex.size - idsToRecycle.size

    inline fun <reified T> register() = register(T::class.java)

    fun register(clazz: Class<*>): Long = if(clazz.isPacked) {
        PackedComponentId()
    } else {
        factories[clazz] = clazz.constructors.first()
        ComponentId()
    }.apply {
        registeredComponents[clazz] = this
    }

    private fun allocateId(): Long = idsToRecycle.firstOrNull()?.apply {
        idsToRecycle.remove(this)
    } ?: (entityCounter++ shl idShiftBitCount)

    fun Entity(): Long {
        val allocatedId = allocateId().toEntityId()

        return allocatedId.apply {
            entityIndex[this] = mutableSetOf()
        }
    }
    fun ComponentId(): Long {
        val allocatedId = (entityCounter++ shl idShiftBitCount).toComponentId()

        return allocatedId.apply {
            entityIndex[this] = mutableSetOf()
        }
    }
    fun PackedComponentId(): Long {
        val allocatedId = (entityCounter++ shl idShiftBitCount).toPackedComponentId()

        return allocatedId.apply {
            entityIndex[this] = mutableSetOf()
        }
    }

    fun getEntity(id: EntityId) = if (entityIndex.containsKey(id)) id else null

    fun EntityId.setInstanceOf(other: EntityId) {
        entityIndex[this]!!.add(other.toInstanceOfIdentifier())
    }

    private fun EntityId.toInstanceOfIdentifier() = this or 3L
    private fun Long.toEntityId() = this or 1L
    private fun Long.toComponentId() = this or 2L
    private fun Long.toPackedComponentId() = this or 2L or 4L
}
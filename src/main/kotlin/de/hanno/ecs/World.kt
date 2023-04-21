package de.hanno.ecs

import java.lang.reflect.Constructor

class World(
    val maxEntityCount: Int = 100000,
) {
    @PublishedApi
    internal val archetypes = mutableSetOf<Archetype>()
    internal val instanceOfs = mutableMapOf<Long, Long>()
    internal val registeredComponents = mutableSetOf<Class<*>>()
    internal val factories = mutableMapOf<Class<*>, Constructor<*>>()

    internal val idsToRecycle = mutableListOf<Long>()

    internal var entityCounter = 0L

    val entityCount: Int get() = entityCounter.toInt() - idsToRecycle.size

    inline fun <reified T> register() = register(T::class.java)

    fun register(clazz: Class<*>) {
        if (!clazz.isPacked) {
            factories[clazz] = clazz.constructors.first()
        }
        registeredComponents.add(clazz)
    }

    private fun allocateId(): Long = idsToRecycle.firstOrNull()?.apply {
        idsToRecycle.remove(this)
    } ?: (entityCounter++ shl idShiftBitCount)

    fun Entity(): Long = allocateId().toEntityId()

    private fun Long.toEntityId() = this or 1L
}

inline fun <reified A: Any, reified B: Any> World.forEntitiesWith(noinline block: (EntityId, A, B) -> Unit) {
    archetypes.first {
        val c = it.componentClasses
        c.size == 2 && c.contains(A::class.java) && c.contains(B::class.java)
    }?.forEntities(block)
}
package de.hanno.ecs

import java.lang.reflect.Constructor

class World {
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

    fun EntityId.setInstanceOf(target: EntityId) {
        // TODO: Implement me
    }
}

inline fun <reified A: Any> World.forEntitiesWith(noinline block: (EntityId, A) -> Unit) {
    archetypes.filter {
        it.componentClasses.contains(A::class.java)
    }.forEach { it.forEntities(A::class.java, block) }
}

inline fun <reified A: Any, reified B: Any> World.forEntitiesWith(noinline block: (EntityId, A, B) -> Unit) {
    archetypes.filter {
        val c = it.componentClasses
        c.contains(A::class.java) && c.contains(B::class.java)
    }.forEach { it.forEntities(A::class.java, B::class.java, block) }
}
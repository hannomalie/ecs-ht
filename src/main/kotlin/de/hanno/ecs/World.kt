package de.hanno.ecs

import java.lang.reflect.Constructor

class World {
    @PublishedApi
    internal val archetypes = mutableSetOf<Archetype>()
    internal val instanceOfs = mutableMapOf<Int, Int>()
    internal val registeredComponents = mutableSetOf<Class<*>>()
    internal val factories = mutableMapOf<Class<*>, Constructor<*>>()

    internal val idsToRecycle = mutableListOf<Int>()

    internal var entityCounter = 0

    val entityCount: Int get() = entityCounter - idsToRecycle.size

    inline fun <reified T> register() = register(T::class.java)

    fun register(clazz: Class<*>) {
        if (!clazz.isPacked) {
            factories[clazz] = clazz.constructors.first()
        }
        registeredComponents.add(clazz)
    }

    private fun allocateId(): Int = idsToRecycle.firstOrNull()?.apply {
        idsToRecycle.remove(this)
    } ?: entityCounter++

    fun Entity(): Int = allocateId()

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
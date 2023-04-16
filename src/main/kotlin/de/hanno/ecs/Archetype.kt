package de.hanno.ecs

interface Archetype<T> {
    val componentClass: Class<T>

    val id: Long
    fun update(entityId: EntityId, component: T) {}

    fun updateAlive() {}

    fun createFor(entityId: EntityId)
    fun deleteFor(entityId: EntityId)

    fun correspondsTo(clazz: Class<*>) = clazz.isAssignableFrom(componentClass)
    fun getFor(entityId: EntityId): T?
}

abstract class ArchetypeImpl<T>(private val world: World): Archetype<T> {
    override val id = world.entityCounter++

    protected val components = mutableMapOf<Long, T>()

    init {
        world.archetypes.add(this)
    }

    override fun updateAlive() = world.run {
        components.filterAlive().forEach { (entityId, component) ->
            update(entityId, component)
        }
    }

    override fun deleteFor(entityId: EntityId) {
        components.remove(entityId)
    }

    override fun getFor(entityId: EntityId): T? = components[entityId]
}

abstract class PackedArchetype<T>(world: World): Archetype<T> {
    override val id = world.entityCounter++

    init {
        world.archetypes.add(this)
    }

    abstract fun on(entityId: EntityId, block: T.() -> Unit)
}

context(World)
fun <T> MutableMap<Long, T>.filterAlive() = filterKeys {
    it.isAlive
}

package de.hanno.ecs

internal var archetypeCounter = 0

interface Archetype<T: Component> {
    val index: Int
    fun update(entityId: EntityId, component: T) {}

    context(World)
    fun updateAlive()

    fun createFor(entityId: Entity)
    fun deleteFor(entityId: Entity)
}

abstract class ArchetypeImpl<T: Component>: Archetype<T> {
    override val index = archetypeCounter++

    protected val components = mutableMapOf<Int, T>()

    context(World)
    override fun updateAlive() {
        components.filterAlive().forEach { (entityId, component) ->
            update(EntityId(entityId), component)
        }
    }

    override fun deleteFor(entityId: Entity) {
        components.remove(entityId.idPart.toInt())
    }
}

abstract class PackedArchetype<T: Component>: Archetype<T> {
    override val index = archetypeCounter++
}

context(World)
fun <T: Component> MutableMap<Int, T>.filterAlive() = filterKeys {
    EntityId(it).isAlive
}

package de.hanno.ecs

internal var archetypeCounter = 0

interface Archetype<T: BaseComponent> {
    val componentClass: Class<T>

    val index: Int
    fun update(entityId: EntityId, component: T) {}

    context(World)
    fun updateAlive() {}

    fun createFor(entityId: Entity)
    fun deleteFor(entityId: Entity)

    fun correspondsTo(clazz: Class<*>) = clazz.isAssignableFrom(componentClass)
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

    fun getFor(entityId: Entity): T? = components[entityId.idPart.toInt()]
}

abstract class PackedArchetype<T: PackedComponent>: Archetype<T> {
    override val index = archetypeCounter++

    context(World)
    abstract fun on(entityId: Entity, block: T.() -> Unit)
    context(World)
    abstract fun getFor(entityId: Entity): T?
}

context(World)
fun <T: BaseComponent> MutableMap<Int, T>.filterAlive() = filterKeys {
    EntityId(it).isAlive
}

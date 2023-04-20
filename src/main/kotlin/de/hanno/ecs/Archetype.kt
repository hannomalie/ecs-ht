package de.hanno.ecs

interface Archetype {
    val componentClasses: Set<Class<*>>
    val componentIds: Set<Long>

    val id: Long

    fun createFor(entityId: EntityId)
    fun deleteFor(entityId: EntityId)

    fun update() {}

    fun correspondsTo(clazz: Class<*>) = componentClasses.any { clazz == it }
    fun getFor(entityId: EntityId): List<*>?
}


abstract class ArchetypeImpl(private val world: World): Archetype {
    override val id = world.run { ComponentId() }

    protected val components: MutableMap<Long, List<Any>> = mutableMapOf()
    override val componentIds: Set<Long> = mutableSetOf()

    init {
        world.archetypes[componentIds] = this
    }

    override fun deleteFor(entityId: EntityId) {
        components.remove(entityId)
    }

    override fun getFor(entityId: EntityId): List<*>? = components[entityId]
}

interface PackedComponent
abstract class PackedArchetype<T: PackedComponent>(private val clazz: Class<T>, world: World): Archetype {
    override val id = world.run { ComponentId() }

    init {
        world.archetypes[componentIds] = this
    }

    abstract fun on(entityId: EntityId, block: T.() -> Unit)

    abstract fun getPackedFor(entityId: EntityId): T?

    override fun correspondsTo(clazz: Class<*>) = clazz == this.clazz
}

context(World)
fun <T> MutableMap<Long, T>.filterAlive() = filterKeys {
    it.isAlive
}

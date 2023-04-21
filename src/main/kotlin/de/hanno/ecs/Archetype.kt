package de.hanno.ecs

interface Archetype {
    val componentClasses: Set<Class<*>>

    fun createFor(entityId: EntityId)
    fun createFor(entityId: EntityId, currentComponentes: List<Any>)
    fun deleteFor(entityId: EntityId)

    fun update() {}

    fun correspondsTo(clazz: Class<*>) = componentClasses.any { clazz == it }
    fun getFor(entityId: EntityId): List<Any>?
    fun has(entity: EntityId): Boolean

    fun <A: Any> forEntities(block: (EntityId, A) -> Unit)
    fun <A: Any, B: Any> forEntities(block: (EntityId, A, B) -> Unit)
}


abstract class ArchetypeImpl(private val world: World): Archetype {
    protected val components: MutableMap<Long, List<Any>> = mutableMapOf()

    override fun has(entity: EntityId): Boolean = components.containsKey(entity)
    init {
        world.archetypes.add(this)
    }

    override fun deleteFor(entityId: EntityId) {
        components.remove(entityId)
    }

    override fun getFor(entityId: EntityId): List<Any>? = components[entityId]

    override fun <A: Any> forEntities(block: (EntityId, A) -> Unit) {
        components.forEach {
            val components = it.value
            block(it.key, components[0] as A)
        }
    }

    override fun <A: Any, B: Any> forEntities(block: (EntityId, A, B) -> Unit) {
        components.forEach {
            val components = it.value
            block(it.key, components[0] as A, components[1] as B)
        }
    }
}

interface PackedComponent
abstract class PackedArchetype<T: PackedComponent>(private val clazz: Class<T>, world: World): Archetype {
    init {
        world.archetypes.add(this)
    }

    abstract fun on(entityId: EntityId, block: T.() -> Unit)

    abstract fun getPackedFor(entityId: EntityId): T?

    override fun correspondsTo(clazz: Class<*>) = clazz == this.clazz
}

context(World)
fun <T> MutableMap<Long, T>.filterAlive() = filterKeys {
    it.isAlive
}

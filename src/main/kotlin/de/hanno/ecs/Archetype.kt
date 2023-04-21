package de.hanno.ecs

import java.lang.IllegalStateException

interface Archetype {
    val componentClasses: Set<Class<*>>

    fun createFor(entityId: EntityId)
    fun createFor(entityId: EntityId, currentComponents: List<Any>)
    fun deleteFor(entityId: EntityId)

    fun update() {}

    fun correspondsTo(clazz: Class<*>) = componentClasses.any { clazz == it }
    fun getFor(entityId: EntityId): List<Any>?
    fun has(entity: EntityId): Boolean

    fun <A: Any> forEntities(classA: Class<A>, block: (EntityId, A) -> Unit)
    fun <A: Any, B: Any> forEntities(classA: Class<A>, classB: Class<B>, block: (EntityId, A, B) -> Unit)
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

    override fun <A: Any> forEntities(classA: Class<A>, block: (EntityId, A) -> Unit) {
        components.forEach {
            val components = it.value
            block(it.key, components.filterIsInstance(classA).first())
        }
    }

    override fun <A: Any, B: Any> forEntities(classA: Class<A>, classB: Class<B>, block: (EntityId, A, B) -> Unit) {
        components.forEach {
            val components = it.value
            block(it.key, components.filterIsInstance(classA).first(), components.filterIsInstance(classB).first())
        }
    }
}

abstract class SingleComponentArchetypeImpl(private val world: World, protected val componentClazz: Class<out Any>): Archetype {
    protected val components: MutableMap<Long, Any> = mutableMapOf() // TODO: Boxing happens here, implement a LongMap

    override fun has(entity: EntityId): Boolean = components.containsKey(entity)
    init {
        world.archetypes.add(this)
    }

    override fun deleteFor(entityId: EntityId) {
        components.remove(entityId)
    }

    override fun getFor(entityId: EntityId): List<Any>? = listOf(components[entityId]!!)

    override fun <A: Any> forEntities(classA: Class<A>, block: (EntityId, A) -> Unit) {
        if(classA != componentClazz) return

        components.forEach {
            block(it.key, it.value as A)
        }
    }

    override fun <A: Any, B: Any> forEntities(classA: Class<A>, classB: Class<B>, block: (EntityId, A, B) -> Unit) {
        throw IllegalStateException("Should never iterate multiple components in this kind of archetype (single component)")
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

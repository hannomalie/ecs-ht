package de.hanno.ecs

import java.lang.IllegalStateException

typealias EntityId = Int
fun EntityId(int: Int) = int

context(World)
fun EntityId.has(componentClass: Class<*>): Boolean = archetypes.any { it.componentClasses.contains(componentClass) && it.has(this) }

context(World)
fun <T> EntityId.get(clazz: Class<T>): T? {
    val archetype = if(clazz.isPacked) getPackedArchetype(clazz as Class<PackedComponent>) else getArchetype(clazz)

    val resolvedComponent = archetype.getFor(this)?.firstOrNull { clazz.isAssignableFrom(it.javaClass) } as T?

    return if (resolvedComponent == null) {
        instanceOfs[this]?.get(clazz)
    } else {
        resolvedComponent
    }
}

context(World)
inline fun <reified T: PackedComponent> EntityId.on(noinline block: T.() -> Unit) {
    on(T::class.java, block)
}

context(World)
fun <T : PackedComponent> EntityId.on(clazz: Class<T>, block: T.() -> Unit) {
    archetypes.filterIsInstance<PackedArchetype<T>>().firstOrNull { it.correspondsTo(clazz) }?.on(this, block)
}

context(World)
inline fun <reified T> EntityId.get(): T? = get(T::class.java)

context(World)
fun EntityId.add(clazz: Class<*>) {
    if(!registeredComponents.contains(clazz)) {
        register(clazz)
    }
    val currentArchetype = archetypes.firstOrNull { it.has(this) }
    move(currentArchetype, clazz)
}

context(World)
fun EntityId.move(currentArchetype: Archetype?, clazz: Class<out Any>) {
    val currentComponents = currentArchetype?.getFor(this) ?: emptyList()
    val componentClasses = if (currentArchetype == null) setOf(clazz) else currentArchetype.componentClasses + clazz

    val archetype = getOrCreateArchetype(componentClasses)

    archetype.createFor(this, currentComponents)
    currentArchetype?.deleteFor(this)
    // TODO: Move over old components
}

context(World)
@JvmName("addPacked")
fun <T: PackedComponent> EntityId.add(clazz: Class<T>) {
    if(!registeredComponents.contains(clazz)) {
        register(clazz)
    }

    val archetype = archetypes.first { it.correspondsTo(clazz) }
    archetype.createFor(this)
    // TODO: Move over old components?
}

val Class<*>.isPacked get() = PackedComponent::class.java.isAssignableFrom(this)

context(World)
// TODO: Is it okay to take the first here?
private fun EntityId.getArchetype(clazz: Class<*>) = archetypes.first { it.correspondsTo(clazz) && it.has(this) }

context(World)
private fun EntityId.getOrCreateArchetype(componentClasses: Set<Class<out Any>>): Archetype = archetypes.firstOrNull {
    it.componentClasses == componentClasses
} ?: if (componentClasses.size == 1) {
    SingleComponentArchetypeImpl(this@World, componentClasses.first())
} else {
    ArchetypeImpl(this@World, componentClasses)
}

context(World)
private fun <T : PackedComponent> EntityId.getPackedArchetype(componentClass: Class<T>) = archetypes.firstOrNull { archetype ->
    archetype.correspondsTo(componentClass)
} ?: throw IllegalStateException("No archetype found for component type $componentClass")

context(World)
private fun <T : PackedComponent> EntityId.getOrCreatePackedArchetype(componentClass: Class<T>): Archetype {
    return archetypes.firstOrNull { it.componentClasses.contains(componentClass) } ?: run {
        // TODO: Currently, need to be registered manually, make it automatically
        throw IllegalStateException(
            "Please manually register an archetyp for $componentClass until it's implemented automatically"
        )
    }
}

context(World)
fun EntityId.delete() {
    archetypes.filter { it.has(this) }.forEach { it.deleteFor(this) }
    idsToRecycle.add(this)
}

context(World)
fun EntityId.remove(componentClass: Class<*>) {
    val currentArchetype = archetypes.filter { it.has(this) }.first { it.correspondsTo(componentClass) }
    val currentComponents = currentArchetype.getFor(this)
    currentArchetype.deleteFor(this)
    val leftOverComponents = currentComponents?.map { it.javaClass }?.filterNot { it == componentClass }?.toSet() ?: emptySet()
    val newArchetype = getOrCreateArchetype(leftOverComponents)
    newArchetype.createFor(this, currentComponents ?: emptyList())
}

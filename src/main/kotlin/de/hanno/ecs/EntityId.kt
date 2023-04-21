package de.hanno.ecs

import java.lang.IllegalStateException

internal val idShiftBitCount = 32
internal val generationShiftBitCount = 16

typealias EntityId = Long
fun EntityId(int: Int): Long = (int.toLong() shl idShiftBitCount) or 1L
val EntityId.idPart: Long get() = this shr idShiftBitCount
fun EntityId.toBinaryString(): String = toString(2)

fun ComponentId(int: Int): Long = (int.toLong() shl idShiftBitCount) or 2L
fun PackedComponentId(int: Int): Long = (int.toLong() shl idShiftBitCount) or 2L or 4L

val Long.isEntity: Boolean get() = this[0]

val Long.isComponent: Boolean get() = this[1] && !this[0]
val Long.isPackedComponent: Boolean get() = isComponent && this[2]

context(World)
fun EntityId.getBinaryStrings(): String {
    return "       " + toBinaryString() + "\n" +
            "id   : " + idPart.shortBinaryString + "\n"
}

val Long.binaryString get() = toString(2).padStart(Long.SIZE_BITS, '0')
val Long.shortBinaryString get() = toString(2).padStart(Int.SIZE_BITS, '0')

context(World)
fun EntityId.has(componentClass: Class<*>): Boolean = archetypes.any { it.componentClasses.contains(componentClass) && it.has(this) }

context(World)
fun <T> EntityId.get(clazz: Class<T>): T? {
    val archeType = if(clazz.isPacked) getPackedArchetype(clazz as Class<PackedComponent>) else getArchetype(clazz)

    val resolvedComponent = archeType.getFor(this)?.firstOrNull { clazz.isAssignableFrom(it.javaClass) } as T?

    return if(resolvedComponent != null) {
        resolvedComponent
    } else {
        instanceOfs[this]?.get(clazz)
    }
}

context(World)
inline fun <reified T: PackedComponent> EntityId.on(noinline block: T.() -> Unit) {
    on(T::class.java, block)
}

context(World)
fun <T: PackedComponent> EntityId.on(clazz: Class<T>, block: T.() -> Unit) {
    val archeType = archetypes.filterIsInstance<PackedArchetype<T>>().firstOrNull { it.correspondsTo(clazz) }

    archeType?.on(this, block)
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
private fun EntityId.getArchetypes() = archetypes.filter { it.has(this) }

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
private fun <T : PackedComponent> EntityId.getPackedArchetype(componentClass: Class<T>) = archetypes.firstOrNull { archeType ->
    archeType.correspondsTo(componentClass)
} ?: throw IllegalStateException("No archetype found for component type $componentClass")

context(World)
private fun <T: PackedComponent> EntityId.getOrCreatePackedArchetype(componentClass: Class<T>): Archetype {
    val archeType = archetypes.firstOrNull { it.componentClasses.contains(componentClass) } ?: run {
//        val newArchetype = object : PackedArchetype<T>(componentClass, this@World) {
//            override fun on(entityId: EntityId, block: T.() -> Unit) {
//                TODO("Not yet implemented")
//            }
//
//            override fun getPackedFor(entityId: EntityId): T? {
//                TODO("Not yet implemented")
//            }
//
//            override val componentClasses: List<Class<*>> = listOf(componentClass)
//
//            override fun createFor(entityId: EntityId) {
////                TODO("Not yet implemented")
//            }
//
//            override fun deleteFor(entityId: EntityId) {
//                TODO("Not yet implemented")
//            }
//
//            override fun getFor(entityId: EntityId): List<*>? {
//                TODO("Not yet implemented")
//            }
//        }
//        archetypes.add(newArchetype)
//        newArchetype

        // TODO: Currently, need to be registered manually, make it automatically
        throw IllegalStateException(
            "Please manually register an archetyp for $componentClass until it's implemented automatically"
        )
    }
    return archeType
}

context(World)
fun EntityId.delete() {
    if (isAlive) {
        archetypes.filter { it.has(this) }.forEach { it.deleteFor(this) }
        idsToRecycle.add(this) // TODO: Increment generation here
    }
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

context(World)
val EntityId.isAlive: Boolean
    get() {
        val expectedGeneration = potentiallyOutdatedGeneration
        val actualGeneration = generation

        return expectedGeneration == actualGeneration
    }
context(World)
val EntityId.generation
    get() = this shr generationShiftBitCount

private val EntityId.potentiallyOutdatedGeneration get() = this shr generationShiftBitCount
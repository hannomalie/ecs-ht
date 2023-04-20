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

val Long.isInstanceOf: Boolean get() = this[1] && this[1]
val Long.targetInstance: Long get() = (idPart shl idShiftBitCount ) or 1L

context(World)
fun EntityId.getBinaryStrings(): String {
    return "       " + toBinaryString() + "\n" +
            "id   : " + idPart.shortBinaryString + "\n"
}

val Long.binaryString get() = toString(2).padStart(Long.SIZE_BITS, '0')
val Long.shortBinaryString get() = toString(2).padStart(Int.SIZE_BITS, '0')

context(World)
fun EntityId.has(componentClass: Class<*>): Boolean = entityIndex[this]?.contains(registeredComponents[componentClass]!!) ?: false

context(World)
fun <T> EntityId.get(clazz: Class<T>): T? {
    val archeType = if(clazz.isPacked) getPackedArchetype(clazz as Class<PackedComponent>) else getArchetype(getNonPackedComponentIds())

    val resolvedComponent = archeType.getFor(this)?.firstOrNull { clazz.isAssignableFrom(it!!.javaClass) } as T?

    return if(resolvedComponent != null) {
        resolvedComponent
    } else {
        val potentialInstanceOfs = entityIndex[this]?.filter { it.isInstanceOf } ?: emptyList()
        val potentialComponents = potentialInstanceOfs.mapNotNull {
            archeType.getFor(it.targetInstance)?.firstOrNull { clazz.isAssignableFrom(it!!.javaClass) } as T?
        }
        potentialComponents.firstOrNull()
    }
}

context(World)
inline fun <reified T: PackedComponent> EntityId.on(noinline block: T.() -> Unit) {
    on(T::class.java, block)
}

context(World)
fun <T: PackedComponent> EntityId.on(clazz: Class<T>, block: T.() -> Unit) {
    val archeType = archetypes.values.filterIsInstance<PackedArchetype<T>>().firstOrNull { it.correspondsTo(clazz) }

    archeType?.on(this, block)
}

context(World)
inline fun <reified T> EntityId.get(): T? = get(T::class.java)

context(World)
fun EntityId.add(clazz: Class<*>) {
    val componentId = registeredComponents[clazz] ?: run {
        register(clazz)
    }
    entityIndex[this]!!.add(componentId)

    val archeType = getOrCreateArchetype(getNonPackedComponentIds())
    archeType.createFor(this)
    // TODO: Move over old components
}
context(World)
@JvmName("addPacked")
fun <T: PackedComponent> EntityId.add(clazz: Class<T>) {
    val componentId = registeredComponents[clazz] ?: run {
        register(clazz)
    }
    entityIndex[this]!!.add(componentId)

    val archetype = getPackedArchetype(setOf(componentId))
    archetype.createFor(this)

    // TODO: Move over old components
}

val Class<*>.isPacked get() = PackedComponent::class.java.isAssignableFrom(this)

context(World)
private fun EntityId.getArchetype(componentIds: Set<Long>) = archetypes[componentIds]
    ?: throw IllegalStateException(
        "No archetype found for componentIds ${componentIds.map { componentId -> registeredComponents.entries.first { it.value == componentId } }}"
    )

context(World)
private fun EntityId.getOrCreateArchetype(componentIds: Set<Long>): Archetype {
    val archeType = archetypes[componentIds] ?: run {
        val newArchetype = object : ArchetypeImpl(this@World) {
            override val componentClasses: Set<Class<*>> = componentIds.map { id -> registeredComponents.entries.first { it.value == id }.key }.toSet()
            override val componentIds = componentIds

            override fun createFor(entityId: EntityId) {
                this.components.put(entityId, this.componentClasses.map {
                    factories[it]!!.newInstance()
                })
            }
        }
        archetypes[componentIds] = newArchetype
        newArchetype
    }
    return archeType
}

context(World)
private fun <T : PackedComponent> EntityId.getPackedArchetype(componentClass: Class<T>) = archetypes.values.firstOrNull { archeType ->
    archeType.correspondsTo(componentClass)
} ?: throw IllegalStateException("No archetype found for component type $componentClass")

context(World)
private fun EntityId.getPackedArchetype(componentId: Set<Long>) = archetypes[componentId] ?: throw IllegalStateException("No archetype found for component type with id $componentId (${registeredComponents.entries.first { it.value == componentId.first() }})")

context(World)
private fun <T: PackedComponent> EntityId.getOrCreatePackedArchetype(componentClass: Class<T>): Archetype {
    val archeType = archetypes[setOf(registeredComponents[componentClass])] ?: run {
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
private fun EntityId.getNonPackedComponentIds(): Set<Long> {
    val allIds = entityIndex[this]!!
    val result = mutableSetOf<Long>()
    allIds.forEach { if(it.isComponent && !it.isPackedComponent) { result.add(it) } }
    return result
}

context(World)
fun EntityId.delete() {
    if (isAlive) {
        entityIndex.remove(this)
        idsToRecycle.add(this) // TODO: Increment generation here
    }
}

context(World)
fun EntityId.remove(componentClass: Class<*>) {
    entityIndex[this]!!.remove(registeredComponents[componentClass]!!)
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
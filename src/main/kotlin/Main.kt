import de.hanno.ecs.*
import java.nio.ByteBuffer

fun main() {
    repeat(1000) {
        val archeTypes = listOf(
            PositionArchetype(),
            PositionVelocityArchetype(),
            PositionVelocityPackedArchetype(),
        )

        World(archeTypes = archeTypes).apply {
            run {
                val start = System.currentTimeMillis()
                repeat(maxEntityCount) {
                    Entity().apply {
                        add(archeTypes[0])
                        add(archeTypes[1])
                        add(archeTypes[2])
                    }
                }
                println("Creating $entityCount entities took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                archeTypes.forEach { it.updateAlive() }
                println("Update $entityCount entities took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                repeat(maxEntityCount) {
                    val entity = getEntity(it)!!
                    entity.get(PositionComponent::class.java)!!.apply {
                        a += 1
                    }
                }
                println("Accessing $entityCount normal components took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                repeat(maxEntityCount) {
                    val entity = getEntity(it)!!
                    entity.on<PositionVelocityPacked> {
                        a += 1
                    }
                }
                println("Accessing $entityCount packed components with block took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                repeat(maxEntityCount) {
                    val entity = getEntity(it)!!
                    entity.get<PositionVelocityPacked>()!!.apply {
                        a += 1
                    }
                }
                println("Accessing $entityCount packed components normally took ${System.currentTimeMillis() - start} ms")
            }
        }
    }
}

data class PositionComponent(var a: Int) : Component
class PositionArchetype : ArchetypeImpl<PositionComponent>() {
    override val componentClass = PositionComponent::class.java

    override fun createFor(entityId: Entity) {
        components[entityId.idPart.toInt()] = PositionComponent(5)
    }
    override fun update(entityId: EntityId, component: PositionComponent) {
        component.a += 1
    }
}

interface Position {
    var a: Int
}

interface Velocity {
    var b: Int
}

data class PositionVelocity(
    override var a: Int = 5,
    override var b: Int = 5,
) : Position, Velocity, Component

class PositionVelocityArchetype : ArchetypeImpl<PositionVelocity>() {
    override val componentClass = PositionVelocity::class.java

    override fun createFor(entityId: Entity) {
        components[entityId.idPart.toInt()] = PositionVelocity()
    }
    override fun update(entityId: EntityId, component: PositionVelocity) {
        component.a += 1
        component.b -= 1
    }
}

interface PositionVelocityPacked: Position, Velocity, PackedComponent

class PositionVelocityPackedArchetype: PackedArchetype<PositionVelocityPacked>() {
    override val componentClass = PositionVelocityPacked::class.java
    private val entities = mutableMapOf<Int, Int>()
    private var currentIndex = 0
    private val buffer = ByteBuffer.allocateDirect(Int.MAX_VALUE)

    private val slidingWindow = object: PositionVelocityPacked {
        override var a: Int
            get() = buffer.getInt(baseOffset)
            set(value) {
                buffer.putInt(baseOffset, value)
            }
        override var b: Int
            get() = buffer.getInt(baseOffset + Int.SIZE_BYTES)
            set(value) {
                buffer.putInt(baseOffset + Int.SIZE_BYTES, value)
            }

        private val baseOffset get() = currentIndex * (2 * Int.SIZE_BYTES)

        override fun toString(): String = "PositionVelocityPacked{a:$a, b:$b}"
    }

    override fun createFor(entityId: Entity) {
        entities[entityId.idPart.toInt()] = entities.size
    }

    override fun deleteFor(entityId: Entity) {
        entities.remove(entityId.idPart.toInt())
    }

    context(World)
    override fun on(entityId: Entity, block: PositionVelocityPacked.() -> Unit) {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)
        if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow.block()
            }
        }
    }
    context(World)
    override fun getFor(entityId: Entity): PositionVelocityPacked? {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)

        return if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow
            } else null
        } else null
    }

    context(World)
    override fun updateAlive() {
        currentIndex = 0
        buffer.position(0)

        entities.keys.forEach { entityId ->
            val entityId = EntityId(entityId)
            if(entityId.isAlive) {
                update(entityId, slidingWindow)
            }
            currentIndex++
        }
    }

    override fun update(entityId: EntityId, component: PositionVelocityPacked) {
        component.a += 1
        component.b += 2
    }
}
import de.hanno.ecs.*
import java.nio.ByteBuffer

fun main() {
    repeat(1000) {

        World().apply {
            val archeTypes = listOf(
                PositionArchetype(this),
                PositionVelocityArchetype(this),
                PositionVelocityPackedArchetype(this),
            )
            val entities = run {
                val start = System.currentTimeMillis()
                val entities = (0 until maxEntityCount).map {
                    Entity().apply {
                        add(archeTypes[0])
                        add(archeTypes[1])
                        add(archeTypes[2])
                    }
                }
                println("Creating $maxEntityCount entities took ${System.currentTimeMillis() - start} ms")
                entities
            }
            val entityCount = entities.size
            run {
                val start = System.currentTimeMillis()
                archeTypes.forEach { it.updateAlive() }
                println("Update $entityCount entities took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                entities.forEach { entity ->
                    entity.get(PositionComponent::class.java)!!.apply {
                        a += 1
                    }
                }
                println("Accessing $entityCount normal components took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                entities.forEach { entity ->
                    entity.on<PositionVelocityPacked> {
                        a += 1
                    }
                }
                println("Accessing $entityCount packed components with block took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                entities.forEach { entity ->
                    entity.get<PositionVelocityPacked>()!!.apply {
                        a += 1
                    }
                }
                println("Accessing $entityCount packed components normally took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                entityIndex.keys.forEach { entity ->
                    entity.get<PositionVelocityPacked>()!!.apply {
                        a += 1
                    }
                }
                println("Accessing $entityCount packed components from world normally took ${System.currentTimeMillis() - start} ms")
            }
        }
    }
}

data class PositionComponent(var a: Int)
class PositionArchetype(world: World) : ArchetypeImpl<PositionComponent>(world) {
    override val componentClass = PositionComponent::class.java

    override fun createFor(entityId: EntityId) {
        components[entityId] = PositionComponent(5)
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
) : Position, Velocity

class PositionVelocityArchetype(world: World) : ArchetypeImpl<PositionVelocity>(world) {
    override val componentClass = PositionVelocity::class.java

    override fun createFor(entityId: EntityId) {
        components[entityId] = PositionVelocity()
    }
    override fun update(entityId: EntityId, component: PositionVelocity) {
        component.a += 1
        component.b -= 1
    }
}

interface PositionVelocityPacked: Position, Velocity

class PositionVelocityPackedArchetype(private val world: World): PackedArchetype<PositionVelocityPacked>(world) {
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

    override fun createFor(entityId: EntityId) {
        entities[entityId.idPart.toInt()] = entities.size
    }

    override fun deleteFor(entityId: EntityId) {
        entities.remove(entityId.idPart.toInt())
    }

    override fun on(entityId: EntityId, block: PositionVelocityPacked.() -> Unit) = world.run {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)
        if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow.block()
            }
        }
    }
    override fun getFor(entityId: EntityId): PositionVelocityPacked? = world.run {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)

        return if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow
            } else null
        } else null
    }

    override fun updateAlive() = world.run {
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
import de.hanno.ecs.*
import java.nio.ByteBuffer

fun main() {
    repeat(10) {
        val archeTypes = listOf(
            PositionArchetype,
            PositionVelocityArchetype,
            PositionVelocityPackedArchetype,
        )

        World(archeTypes).apply {
            run {
                val start = System.currentTimeMillis()
                repeat(maxEntityCount) {
                    Entity().apply {
                        add(PositionVelocityArchetype)
                        add(PositionArchetype)
                        add(PositionVelocityPackedArchetype)
                    }
                }
                println("Creating $entityCount entities took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                archeTypes.forEach { it.updateAlive() }
                println("Update $entityCount entities took ${System.currentTimeMillis() - start} ms")
            }
        }
    }
}

data class PositionComponent(var a: Int) : Component
object PositionArchetype : ArchetypeImpl<PositionComponent>() {
    override fun createFor(entityId: Entity) {
        components[entityId.idPart.toInt()] = PositionComponent(5)
    }
    override fun update(entityId: EntityId, component: PositionComponent) {
        component.a += 1
    }
}

interface Position : Component {
    var a: Int
}

interface Velocity : Component {
    var b: Int
}

data class PositionVelocity(
    override var a: Int = 5,
    override var b: Int = 5,
) : Position, Velocity

object PositionVelocityArchetype : ArchetypeImpl<PositionVelocity>() {
    override fun createFor(entityId: Entity) {
        components[entityId.idPart.toInt()] = PositionVelocity()
    }
    override fun update(entityId: EntityId, component: PositionVelocity) {
        component.a += 1
        component.b -= 1
    }
}

interface PositionVelocityPacked: Position, Velocity

object PositionVelocityPackedArchetype: PackedArchetype<PositionVelocityPacked>() {
    private val entities = mutableListOf<Int>()
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
        entities.add(entityId.idPart.toInt())
    }

    override fun deleteFor(entityId: Entity) {
        entities.remove(entityId.idPart.toInt())
    }

    context(World)
    override fun updateAlive() {
        currentIndex = 0
        buffer.position(0)

        entities.forEach { entityId ->
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
//        println("Update of ${entityId.idPart} with $component")
    }
}
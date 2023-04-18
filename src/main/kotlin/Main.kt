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
                archeTypes.forEach { it.update() }
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
class PositionArchetype(world: World) : ArchetypeImpl(world) {
    override val componentClasses = listOf(PositionComponent::class.java)

    override fun createFor(entityId: EntityId) {
        components[entityId] = listOf(PositionComponent(5))
    }
    override fun update() {
        components.forEach { (_, componentsForEntity) ->
            val positionComponent = componentsForEntity[0] as PositionComponent
            positionComponent.a += 1
        }
    }
}

data class Velocity(var b: Int)

class PositionVelocityArchetype(world: World) : ArchetypeImpl(world) {
    override val componentClasses = listOf(PositionComponent::class.java, Velocity::class.java)

    override fun createFor(entityId: EntityId) {
        components[entityId] = listOf(PositionComponent(5), Velocity(2))
    }

    override fun update() {
        components.forEach { (_, componentsForEntity) ->
            componentsForEntity.forEach {
                when(it) {
                    is PositionComponent -> it.a += 1
                    is Velocity -> it.b -= 1
                }
            }
        }
    }
}

interface PositionVelocityPacked: PackedComponent {
    var a: Int
    var b: Int
}

class PositionVelocityPackedArchetype(private val world: World): PackedArchetype<PositionVelocityPacked>(PositionVelocityPacked::class.java, world) {
    override val componentClasses = listOf(PositionVelocityPacked::class.java)
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
    override fun getFor(entityId: EntityId): List<*>? = world.run {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)

        return if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                listOf(slidingWindow)
            } else null
        } else null
    }

    override fun getPackedFor(entityId: EntityId): PositionVelocityPacked? = world.run {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)

        return if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow
            } else null
        } else null
    }

    override fun update() = world.run {
        currentIndex = 0
        buffer.position(0)

        entities.keys.forEach { entityId ->
            val entityId = EntityId(entityId)
            if(entityId.isAlive) {
                getPackedFor(entityId)?.apply {
                    a += 1
                    b += 2
                }
            }
            currentIndex++
        }
    }

}
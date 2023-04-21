import de.hanno.ecs.*
import java.lang.IllegalStateException
import java.nio.ByteBuffer

fun main() {
    repeat(1000) {

        World().apply {
            PositionVelocityPackedArchetype(this)

            val entities = run {
                val start = System.currentTimeMillis()
                val entities = (0 until maxEntityCount).map {
                    Entity().apply {
                        add(PositionComponent::class.java)
                        add(Velocity::class.java)
                        add(PositionVelocityPacked::class.java)
                    }
                }
                println("Creating $maxEntityCount entities took ${System.currentTimeMillis() - start} ms")
                entities
            }
            val entityCount = entities.size
            run {
                val start = System.currentTimeMillis()
                archetypes.forEach { it.update() }
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
                entities.forEach { entity ->
                    entity.get<PositionVelocityPacked>()!!.apply {
                        a += 1
                    }
                }
                println("Accessing $entityCount packed components from world normally took ${System.currentTimeMillis() - start} ms")
            }
            run {
                val start = System.currentTimeMillis()
                var counter = 0
                forEntitiesWith<PositionComponent, Velocity> { _, position, velocity ->
                    position.a++
                    velocity.b++
                    counter++
                }
                println("Updating $counter entities with two components took ${System.currentTimeMillis() - start} ms")
            }
        }
    }
}

data class PositionComponent(var a: Int = 0)

data class Velocity(var b: Int = 1)

interface PositionVelocityPacked: PackedComponent {
    var a: Int
    var b: Int
}

class PositionVelocityPackedArchetype(private val world: World): PackedArchetype<PositionVelocityPacked>(PositionVelocityPacked::class.java, world) {
    init {
        world.register(PositionVelocityPacked::class.java)
    }

    override val componentClasses = setOf(PositionVelocityPacked::class.java)

    private val entities = mutableMapOf<EntityId, Int>()
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
        entities[entityId] = entities.size
    }

    override fun createFor(entityId: EntityId, currentComponentes: List<Any>) {
        // TODO: Figure out if it's ok to ignore currentComponents
        createFor(entityId)
    }

    override fun deleteFor(entityId: EntityId) {
        entities.remove(entityId)
    }

    override fun on(entityId: EntityId, block: PositionVelocityPacked.() -> Unit) = world.run {
        val index = entities.getOrDefault(entityId, null)
        if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow.block()
            }
        }
    }
    override fun getFor(entityId: EntityId): List<Any>? = world.run {
        val index = entities.getOrDefault(entityId, null)

        return if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                listOf(slidingWindow)
            } else null
        } else null
    }

    override fun <A : Any> forEntities(block: (EntityId, A) -> Unit) {
        entities.forEach { block(it.key, getFor(it.key)!![0] as A) }
    }

    override fun <A : Any, B : Any> forEntities(block: (EntityId, A, B) -> Unit) {
        throw IllegalStateException("Packed archetypes should never expect to iterate multiple component rows!")
    }

    override fun has(entity: EntityId): Boolean = entities.containsKey(entity)

    override fun getPackedFor(entityId: EntityId): PositionVelocityPacked? = world.run {
        val index = entities.getOrDefault(entityId, null)

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
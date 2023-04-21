import com.carrotsearch.hppc.IntIntHashMap
import com.carrotsearch.hppc.LongIntHashMap
import de.hanno.ecs.*
import java.nio.ByteBuffer

fun main() {
    val maxEntityCount = 100000

    repeat(1000) {
        println("======================")

        World().apply {
            register(PositionVelocityPacked::class.java)
            PositionVelocityPackedArchetype(this)

            var entities: List<Int>? = null
            timed {
                val result = (0 until maxEntityCount).map {
                    Entity().apply {
                        add(PositionComponent::class.java)
                        if(it.toFloat() / maxEntityCount.toFloat() > 0.5) {
                            add(Velocity::class.java)
                        }
                        if (it.toFloat() / maxEntityCount.toFloat() > 0.75) {
                            add(PositionVelocityPacked::class.java)
                        }
                    }
                }
                entities = result
                "Creating $maxEntityCount entities took %d ms"
            }
            println("=======")

            timed {
                var counter = 0
                entities!!.forEach { entity ->
                    entity.get(PositionComponent::class.java)!!.apply {
                        a += 1
                        counter++
                    }
                }
                "Accessing $counter components with entity.get took %d ms"
            }
            timed {
                var counter = 0
                entities!!.forEach { entity ->
                    entity.get<PositionVelocityPacked>()?.apply {
                        a += 1
                        counter++
                    }
                }
                "Accessing $counter packed components with entity.get took %d ms"
            }
            timed {
                var counter = 0
                entities!!.forEach { entity ->
                    entity.on<PositionVelocityPacked> {
                        a += 1
                        counter++
                    }
                }
                "Accessing $counter packed components with entity.on took %d ms"
            }
            println("=======")
            timed {
                var counter = 0
                forEntitiesWith<PositionVelocityPacked> { _, component ->
                    component.a += 1
                    counter++
                }
                "Accessing $counter packed components with forEntitiesWith<PositionVelocityPacked> took %d ms"
            }
            timed {
                var counter = 0
                forEntitiesWith<PositionComponent> { _, position ->
                    position.a++
                    counter++
                }
                "Updating $counter entities with one component took %d ms"
            }
            timed {
                var counter = 0
                forEntitiesWith<PositionComponent, Velocity> { _, position, velocity ->
                    position.a++
                    velocity.b++
                    counter++
                }
                "Updating $counter entities with two components took %d ms"
            }
            timed {
                var counter = 0
                entities!!.forEach { entity ->
                    entity.remove(PositionComponent::class.java)
                    counter++
                }
                "Removing one component from $counter entities with two components took %d ms"
            }
            timed {
                var counter = 0
                forEntitiesWith<Velocity> { _, velocity ->
                    velocity.b++
                    counter++
                }
                "Updating one component after removing one from $counter entities with two components took %d ms"
            }
        }
    }
}

inline fun timed(block: () -> String) {
    val start = System.currentTimeMillis()
    val message = block()
    val durationMs = System.currentTimeMillis() - start

    println(String.format(message, durationMs))
}
data class PositionComponent(var a: Int = 0)

data class Velocity(var b: Int = 1)

interface PositionVelocityPacked: PackedComponent {
    var a: Int
    var b: Int
}

class PositionVelocityPackedArchetype(private val world: World): PackedArchetype<PositionVelocityPacked>(PositionVelocityPacked::class.java, world) {
    override val componentClasses = setOf(PositionVelocityPacked::class.java)

    private val entities = IntIntHashMap()
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
        entities.put(entityId, entities.size())
    }

    override fun createFor(entityId: EntityId, currentComponents: List<Any>) {
        // TODO: Figure out if it's ok to ignore currentComponents
        createFor(entityId)
    }

    override fun deleteFor(entityId: EntityId) {
        entities.remove(entityId)
    }

    override fun on(entityId: EntityId, block: PositionVelocityPacked.() -> Unit) = world.run {
        val index = entities.getOrDefault(entityId, -1)
        if(index != -1) {
            buffer.position(index * (2 * Int.SIZE_BYTES))
            slidingWindow.block()
        }
    }
    override fun getFor(entityId: EntityId): List<Any>? = world.run {
        val index = entities.getOrDefault(entityId, -1)

        return if(index != -1) {
            buffer.position(index * (2 * Int.SIZE_BYTES))
            listOf(slidingWindow)
        } else null
    }

    override fun <A : Any> forEntities(classA: Class<A>, block: (EntityId, A) -> Unit) {
        entities.forEach { block(it.key, getFor(it.key)!![0] as A) }
    }

    override fun <A : Any, B : Any> forEntities(classA: Class<A>, classB: Class<B>, block: (EntityId, A, B) -> Unit) {
        throw IllegalStateException("Packed archetypes should never expect to iterate multiple component rows!")
    }

    override fun has(entity: EntityId): Boolean = entities.containsKey(entity)

    override fun getPackedFor(entityId: EntityId): PositionVelocityPacked? = world.run {
        val index = entities.getOrDefault(entityId, -1)

        return if(index != -1) {
            buffer.position(index * (2 * Int.SIZE_BYTES))
            slidingWindow
        } else null
    }
}

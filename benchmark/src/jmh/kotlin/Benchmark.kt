package de.hanno.ecs.benchmark

import PositionArchetype
import PositionComponent
import PositionVelocityPacked
import PositionVelocityPackedArchetype
import de.hanno.ecs.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 200, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class CreationBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    open class State {
        lateinit var world: World
        lateinit var positionVelocityPackedArchetype: PositionVelocityPackedArchetype
        lateinit var positionArchetype: PositionArchetype

        @Setup(Level.Trial)
        open fun doSetup() {
            world = World(
                100000,
            )
            positionVelocityPackedArchetype = PositionVelocityPackedArchetype(world)
            positionArchetype = PositionArchetype(world)
        }
    }

    @Benchmark
    fun create_1_entity_1_packed_comp(state: State) = state.world.apply {
        Entity().apply {
            add(state.positionVelocityPackedArchetype)
        }
    }

    @Benchmark
    fun create_10000_entity_1_packed_comp(state: State) = state.world.apply {
        repeat(10000) {
            Entity().apply {
                add(state.positionVelocityPackedArchetype)
            }
        }
    }

    @Benchmark
    fun create_1_entity_1_comp(state: State) = state.world.apply {
        Entity().apply {
            add(state.positionArchetype)
        }
    }

    @Benchmark
    fun create_10000_entity_1_comp(state: State) = state.world.apply {
        repeat(10000) {
            Entity().apply {
                add(state.positionArchetype)
            }
        }
    }

    @Benchmark
    fun get_10000_entities(state:State, blackhole: Blackhole) = state.world.apply {
        repeat(10000) {
            blackhole.consume(getEntity(EntityId(it)))
        }
    }
}
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 200, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class IterationBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    open class State {
        lateinit var world: World
        lateinit var positionVelocityPackedArchetype: PositionVelocityPackedArchetype
        lateinit var positionArchetype: PositionArchetype
        lateinit var blackholePositionArchetype: BlackholePositionArchetype
        lateinit var blackholePositionVelocityPackedArchetype: BlackholePositionVelocityPackedArchetype

        @Setup(Level.Trial)
        open fun doSetup() {
            world = World(
                100000,
            )
            positionVelocityPackedArchetype = PositionVelocityPackedArchetype(world)
            positionArchetype = PositionArchetype(world)
            blackholePositionArchetype = BlackholePositionArchetype(world)
            blackholePositionVelocityPackedArchetype = BlackholePositionVelocityPackedArchetype(world)
            repeat(100000) {
                world.apply {
                    Entity().apply {
                        add(positionArchetype)
                        add(positionVelocityPackedArchetype)
                        add(blackholePositionArchetype)
                        add(blackholePositionVelocityPackedArchetype)
                    }
                }
            }
        }
    }

    @Benchmark
    fun get_1_entity_1_comp(state: State, blackhole: Blackhole) = state.world.apply {
        val comp = getEntity(0)!!.get(PositionComponent::class.java)
        blackhole.consume(comp)
    }

    @Benchmark
    fun get_100000_entities_1_comp(state: State, blackhole: Blackhole) = state.world.apply {
        repeat(100000) {
            val comp = getEntity(EntityId(it))!!.get(PositionComponent::class.java)
            blackhole.consume(comp)
        }
    }

    @Benchmark
    fun iterate_archetype_100000_entities(state: State, blackhole: Blackhole) = state.world.apply {
        val blackholePositionArchetype = state.blackholePositionArchetype

        blackholePositionArchetype.blackhole = blackhole
        blackholePositionArchetype.updateAlive()
    }

    @Benchmark
    fun iterate_packedarchetype_100000_entities(state: State, blackhole: Blackhole) = state.world.apply {
        val blackholePositionVelocityPackedArchetype = state.blackholePositionVelocityPackedArchetype

        blackholePositionVelocityPackedArchetype.blackhole = blackhole
        blackholePositionVelocityPackedArchetype.updateAlive()
    }

    class BlackholePositionArchetype(world: World) : ArchetypeImpl<PositionComponent>(world) {
        lateinit var blackhole: Blackhole
        override val componentClass = PositionComponent::class.java

        override fun createFor(entityId: EntityId) {
            components[entityId] = PositionComponent(5)
        }
        override fun update(entityId: EntityId, component: PositionComponent) {
            blackhole.consume(component.a)
        }
    }
}

class BlackholePositionVelocityPackedArchetype(world: World): PackedArchetype<PositionVelocityPacked>(world) {
    lateinit var blackhole: Blackhole
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

    context(World)
    override fun on(entityId: EntityId, block: PositionVelocityPacked.() -> Unit) {
        val index = entities.getOrDefault(entityId.idPart.toInt(), null)
        if(index != null) {
            if(entityId.isAlive) {
                buffer.position(index * (2 * Int.SIZE_BYTES))
                slidingWindow.block()
            }
        }
    }
    context(World)
    override fun getFor(entityId: EntityId): PositionVelocityPacked? {
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
        blackhole.consume(component.a)
    }
}

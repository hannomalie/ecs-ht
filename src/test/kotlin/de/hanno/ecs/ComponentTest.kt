package de.hanno.ecs

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.*

class ComponentTest {
    @TestFactory
    fun `component can be added`(): List<DynamicTest> = World().run {
        createArcheTypes()
        register<Component0>()
        (0..3).map {
            DynamicTest.dynamicTest("for $it") {
                val entity = Entity()

                entity.add(Component0::class.java)
                assertTrue(entity.has(Component0::class.java))
            }
        }
    }

    @TestFactory
    fun `component can be removed`(): List<DynamicTest> = World().run {
        createArcheTypes()
        register<Component0>()
        (0..3).map {
            DynamicTest.dynamicTest("for $it") {
                val entity = Entity()

                entity.add(Component0::class.java)
                assertTrue(entity.has(Component0::class.java))
                entity.remove(Component0::class.java)
                assertFalse(entity.has(Component0::class.java))
            }
        }
    }

    @Test
    fun `instancing uses shared component`(): Unit = World().run {
        createArcheTypes()
        register<Component0>()
        register<Component1>()

        val entity0 = Entity()
        entity0.add(Component0::class.java)

        val entity1 = Entity()
        entity1.setInstanceOf(entity0)

        val componentFromEntity0 = entity0.get(Component0::class.java)
        val componentFromEntity1 = entity1.get(Component0::class.java)

        assertNotNull(componentFromEntity0)
        assertNotNull(componentFromEntity1)
        assertEquals(componentFromEntity0, componentFromEntity1)
    }

    @Test
    fun `instancing uses overridden component`(): Unit = World().run {
        createArcheTypes()
        register<Component0>()
        register<Component1>()

        val entity0 = Entity().apply {
            add(Component0::class.java)
            add(Component1::class.java)
        }

        val entity1 = Entity().apply {
            add(Component0::class.java)
            setInstanceOf(entity0)
        }

        val componentFromEntity0 = entity0.get(Component0::class.java)
        val componentFromEntity1 = entity1.get(Component0::class.java)
        val componentSharedFromEntity0 = entity1.get(Component0::class.java)

        assertNotNull(componentFromEntity0)
        assertNotNull(componentFromEntity1)
        assertNotNull(componentSharedFromEntity0)
        assertNotEquals(componentFromEntity0, componentFromEntity1)
    }

    @Test
    fun `component assignment is determined correctly`(): Unit = World().run {
        register<Component0>()
        register<Component1>()
        createArcheTypes()

        val entity = Entity()

        entity.add(Component0::class.java)
        entity.add(Component1::class.java)

        assertTrue(entity.has(Component0::class.java))
        assertTrue(entity.has(Component1::class.java))
    }

    @Test
    fun `components can be registered`(): Unit = World().run {
        register(Component0::class.java)
        register(Component1::class.java)
        createArcheTypes()

        val entity = Entity()

        entity.add(Component0::class.java)
        entity.add(Component1::class.java)

        assertTrue(entity.has(Component0::class.java))
    }
}
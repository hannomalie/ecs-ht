package de.hanno.ecs

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.*

class ComponentTest {
    @TestFactory
    fun `component can be added`(): List<DynamicTest> = World().run {
        createArcheTypes()
        (0..3).map {
            DynamicTest.dynamicTest("for $it") {
                val entity = Entity()
                val archetype = archetypes[it]

                entity.add(archetype)
                assertTrue(entity.has(archetype))
            }
        }
    }

    @TestFactory
    fun `component can be removed`(): List<DynamicTest> = World().run {
        createArcheTypes()
        (0..3).map {
            DynamicTest.dynamicTest("for $it") {
                val entity = Entity()
                val componentType = archetypes[it]

                entity.add(componentType)
                assertTrue(entity.has(componentType))
                entity.remove(componentType)
                assertFalse(entity.has(componentType))
            }
        }
    }

    @Test
    fun `instancing uses shared component`(): Unit = World().run {
        val archeTypes = createArcheTypes()

        val entity0 = Entity()
        entity0.add(archeTypes[0])

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
        val archeTypes = createArcheTypes()

        val entity0 = Entity().apply {
            add(archeTypes[0])
            add(archeTypes[1])
        }

        val entity1 = Entity().apply {
            add(archeTypes[0])
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

    @TestFactory
    fun `component archetype is determined correctly`(): Unit = World().run {
        object: ArchetypeImpl(this) {
            override val componentClasses = listOf(
                Component0::class.java,
                Component1::class.java
            )
            override fun createFor(entityId: EntityId) {
                components[entityId] = listOf(
                    Component0(5), Component1(2)
                )
            }
        }

        val entity = Entity()

        entity.add(Component0::class.java)
        entity.add(Component1::class.java)

        assertTrue(entity.has(archetypes[0]))
    }
}
package de.hanno.ecs

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
}
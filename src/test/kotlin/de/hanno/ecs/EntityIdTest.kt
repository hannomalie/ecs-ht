package de.hanno.ecs

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityIdTest {
    @TestFactory
    fun `id part is recognized`(): List<DynamicTest> = World(archetypes).run {
        (0..20).map {
            dynamicTest("for $it") {
                val entity = Entity()
                assertEquals(it.toLong(), entity.idPart, "Expected ids to be correct:\n" + entity.getBinaryStrings())
            }
        }
    }

    @TestFactory
    fun `component part is recognized`(): List<DynamicTest> = World(archetypes).run {
        val expectedBinaryStringsForIds = arrayOf(
            "00000000000000000000000000000000",
            "00000000000000000000000000000001",
            "00000000000000000000000000000010",
            "00000000000000000000000000000011",
        )
        (0..3).map {
            dynamicTest("for $it") {
                val entity = Entity()
                assertEquals(0L, entity.componentPart)
                assertEquals("00000000000000000000000000000000", entity.componentPart.shortBinaryString, "Expected ids to be correct:\n" + entity.getBinaryStrings())
                assertEquals(expectedBinaryStringsForIds[it], entity.idPart.shortBinaryString)
            }
        }
    }

    @TestFactory
    fun `component can be added`(): List<DynamicTest> = World(archetypes).run {
        val expectedBinaryStringsForIds = arrayOf(
            "00000000000000000000000000000000",
            "00000000000000000000000000000001",
            "00000000000000000000000000000010",
            "00000000000000000000000000000011",
        )
        val expectedBinaryStringsForComponents = arrayOf(
            "00000000000000000000000000000001",
            "00000000000000000000000000000010",
            "00000000000000000000000000000100",
            "00000000000000000000000000001000",
        )
        (0..3).map {
            dynamicTest("for $it") {
                val entity = Entity()
                val archetype = archetypes[it]

                entity.add(archetype)
                assertTrue(entity.has(archetype))
                assertEquals(expectedBinaryStringsForComponents[it], entity.componentPart.shortBinaryString)
                assertEquals(expectedBinaryStringsForIds[it], entity.idPart.shortBinaryString)
            }
        }
    }

    @TestFactory
    fun `component can be removed`(): List<DynamicTest> = World(archetypes).run {
        (0..3).map {
            dynamicTest("for $it") {
                val entity = Entity()
                val componentType = archetypes[it]

                entity.add(componentType)
                assertTrue(entity.has(componentType))
                entity.remove(componentType)
                assertFalse(entity.has(componentType))
                assertEquals("00000000000000000000000000000000", entity.componentPart.shortBinaryString)
            }
        }
    }

    @Test
    fun `entity is alive and dead and recycles`(): Unit = World(archetypes).run {
        (0..3).map {
            dynamicTest("for $it") {
                run {
                    val entity = Entity()
                    assertTrue(entity.isAlive)
                    assertEquals(0, entity.generation)
                    assertEquals(1, entityCount)
                    entity.delete()
                    assertFalse(entity.isAlive)
                    assertEquals(1, entity.generation)
                    assertEquals(0, entityCount)
                    entity.delete()
                    assertFalse(entity.isAlive)
                    assertEquals(1, entity.generation)
                }

                assertEquals(0, entityCount)
                val newEntity = Entity()
                assertTrue(newEntity.isAlive)
                assertEquals(1, newEntity.generation)
                assertEquals(1, entityCount)
            }
        }
    }
}

class Component0(var a: Int = 0) : Component
class Component1(var a: Int = 0) : Component
class Component2(var a: Int = 0) : Component
class Component3(var a: Int = 0) : Component

val archetypes = mutableListOf(
    object: ArchetypeImpl<Component0>() {
        override val componentClass = Component0::class.java
        override fun createFor(entityId: Entity) {
            components[entityId.idPart.toInt()] = Component0(5)
        }
    },
    object: ArchetypeImpl<Component1>() {
        override val componentClass = Component1::class.java
        override fun createFor(entityId: Entity) {
            components[entityId.idPart.toInt()] = Component1(5)
        }
    },
    object: ArchetypeImpl<Component2>() {
        override val componentClass = Component2::class.java
        override fun createFor(entityId: Entity) {
            components[entityId.idPart.toInt()] = Component2(5)
        }
    },
    object: ArchetypeImpl<Component3>() {
        override val componentClass = Component3::class.java
        override fun createFor(entityId: Entity) {
            components[entityId.idPart.toInt()] = Component3(5)
        }
    },
)
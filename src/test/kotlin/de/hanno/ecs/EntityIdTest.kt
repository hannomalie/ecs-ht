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
    fun `id part is recognized`(): List<DynamicTest> = World().run {
        createArcheTypes()
        (0..20).map {
            dynamicTest("for $it") {
                val entity = Entity()
                val expectedId = archetypes.size + it.toLong()
                assertEquals(expectedId, entity.idPart, "Expected ids to be correct:\n" + entity.getBinaryStrings())
            }
        }
    }

    @Test
    fun `entity is alive and dead and recycles`(): Unit = World().run {
        createArcheTypes()
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

    @Test
    fun `id flag flags entity id`() {
        assertTrue(EntityId(0).isEntity)
        assertFalse(0L.isEntity)
    }

    @Test
    fun `component flag flags component id`() {
        assertTrue(ComponentId(0).isComponent)
        assertFalse(0L.isComponent)
    }

    @Test
    fun `instanceOf flag flags entity`() {
        World().run {
            val entity0 = Entity()
            val entity1 = Entity()
            entity1.setInstanceOf(entity0)

            val savedIdentifier = entityIndex[entity1]!![0]
            assertTrue(savedIdentifier.isInstanceOf)
            assertEquals(savedIdentifier.targetInstance, entity0)
        }
    }
}

class Component0(var a: Int = 0)
class Component1(var a: Int = 0)
class Component2(var a: Int = 0)
class Component3(var a: Int = 0)

fun World.createArcheTypes(): MutableList<ArchetypeImpl<*>> = mutableListOf(
    object: ArchetypeImpl<Component0>(this) {
        override val componentClass = Component0::class.java
        override fun createFor(entityId: EntityId) {
            components[entityId] = Component0(5)
        }
    },
    object: ArchetypeImpl<Component1>(this) {
        override val componentClass = Component1::class.java
        override fun createFor(entityId: EntityId) {
            components[entityId] = Component1(5)
        }
    },
    object: ArchetypeImpl<Component2>(this) {
        override val componentClass = Component2::class.java
        override fun createFor(entityId: EntityId) {
            components[entityId] = Component2(5)
        }
    },
    object: ArchetypeImpl<Component3>(this) {
        override val componentClass = Component3::class.java
        override fun createFor(entityId: EntityId) {
            components[entityId] = Component3(5)
        }
    },
)
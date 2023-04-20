package de.hanno.ecs

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface ParentComponent0
interface ParentComponent1
class SubComponent: ParentComponent0, ParentComponent1

class ArcheTypeTest {
    @Test
    fun `archetype corresponds to subclass only`() {
        val archeType = object: Archetype {
            override val componentClasses = setOf(SubComponent::class.java)
            override val componentIds: Set<Long> get() = TODO("Not yet implemented")

            override val id: Long get() = TODO("Not yet implemented")
            override fun createFor(entityId: EntityId) {}
            override fun deleteFor(entityId: EntityId) {}
            override fun getFor(entityId: EntityId) = TODO("Not yet implemented")
        }

        assertTrue(archeType.correspondsTo(SubComponent::class.java))
        assertFalse(archeType.correspondsTo(ParentComponent0::class.java))
        assertFalse(archeType.correspondsTo(ParentComponent1::class.java))
    }

    @Test
    fun `packed archetype corresponds to subclass only`() {
        val archeType = object: Archetype {
            override val componentClasses = setOf(SubComponent::class.java)
            override val componentIds: Set<Long> get() = TODO("Not yet implemented")

            override val id: Long get() = TODO("Not yet implemented")
            override fun createFor(entityId: EntityId) {}
            override fun deleteFor(entityId: EntityId) {}
            override fun getFor(entityId: EntityId) = TODO("Not yet implemented")
        }

        assertTrue(archeType.correspondsTo(SubComponent::class.java))
        assertFalse(archeType.correspondsTo(ParentComponent0::class.java))
        assertFalse(archeType.correspondsTo(ParentComponent1::class.java))
    }
}
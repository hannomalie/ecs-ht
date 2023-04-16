package de.hanno.ecs

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

interface ParentComponent0
interface ParentComponent1
class SubComponent: ParentComponent0, ParentComponent1

class ArcheTypeTest {
    @Test
    fun `archetype corresponds to class and subclasses`() {
        val archeType = object: Archetype<SubComponent> {
            override val componentClass = SubComponent::class.java

            override val id: Long get() = TODO("Not yet implemented")
            override fun updateAlive() {}
            override fun createFor(entityId: EntityId) {}
            override fun deleteFor(entityId: EntityId) {}
        }

        assertTrue(archeType.correspondsTo(SubComponent::class.java))
        assertTrue(archeType.correspondsTo(ParentComponent0::class.java))
        assertTrue(archeType.correspondsTo(ParentComponent1::class.java))
    }
}
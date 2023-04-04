package de.hanno.ecs

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

interface ParentComponent0: Component
interface ParentComponent1: Component
class SubComponent: ParentComponent0, ParentComponent1

class ArcheTypeTest {
    @Test
    fun `archetype corresponds to class and subclasses`() {
        val archeType = object: Archetype<SubComponent> {
            override val componentClass = SubComponent::class.java

            override val index: Int get() = TODO("Not yet implemented")
            context(World) override fun updateAlive() {}
            override fun createFor(entityId: Entity) {}
            override fun deleteFor(entityId: Entity) {}
        }

        assertTrue(archeType.correspondsTo(SubComponent::class.java))
        assertTrue(archeType.correspondsTo(ParentComponent0::class.java))
        assertTrue(archeType.correspondsTo(ParentComponent1::class.java))
    }
}
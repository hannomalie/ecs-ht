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
        val archetype = ArchetypeImpl(World(), setOf(SubComponent::class.java))

        assertTrue(archetype.correspondsTo(SubComponent::class.java))
        assertFalse(archetype.correspondsTo(ParentComponent0::class.java))
        assertFalse(archetype.correspondsTo(ParentComponent1::class.java))
    }

    @Test
    fun `packed archetype corresponds to subclass only`() {
        val archetype = ArchetypeImpl(World(), setOf(SubComponent::class.java))

        assertTrue(archetype.correspondsTo(SubComponent::class.java))
        assertFalse(archetype.correspondsTo(ParentComponent0::class.java))
        assertFalse(archetype.correspondsTo(ParentComponent1::class.java))
    }
}
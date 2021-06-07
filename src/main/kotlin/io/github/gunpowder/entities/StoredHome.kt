package io.github.gunpowder.entities

import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3i
import java.util.*

data class StoredHome(
    val user: UUID,
    val name: String,
    val location: Vec3i,
    val dimension: Identifier
)

package io.github.gunpowder.ext

import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

fun Vec3i.center() = Vec3d.of(this).add(0.5, 0.5, 0.5)

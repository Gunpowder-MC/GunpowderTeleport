/*
 * MIT License
 *
 * Copyright (c) 2020 GunpowderMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.gunpowder.modelhandlers

import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.entities.StoredWarp
import io.github.gunpowder.models.WarpTable
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

object WarpHandler {
    private val db by lazy {
        GunpowderMod.instance.database
    }
    val cache = mutableMapOf<String, StoredWarp>()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        val temp = db.transaction {
            WarpTable.selectAll().map {
                it[WarpTable.name] to StoredWarp(
                        it[WarpTable.name],
                        it[WarpTable.pos],
                        it[WarpTable.dimension])
            }.toMap()
        }.get()
        cache.putAll(temp)
    }

    fun getWarp(name: String): StoredWarp? {
        return cache[name]
    }

    fun getWarps(): Map<String, StoredWarp> {
        return cache.toMap()
    }

    fun delWarp(warp: String): Boolean {
        if (cache.containsKey(warp)) {
            db.transaction {
                WarpTable.deleteWhere {
                    WarpTable.name.eq(warp)
                }
            }
            cache.remove(warp)
            return true
        }
        return false
    }

    fun newWarp(warp: StoredWarp): Boolean {
        if (!cache.containsKey(warp.name)) {
            cache[warp.name] = warp
            db.transaction {
                WarpTable.insert {
                    it[WarpTable.name] = warp.name
                    it[WarpTable.pos] = BlockPos(warp.location)
                    it[WarpTable.dimension] = warp.dimension
                }
            }
            return true
        }
        return false
    }
}

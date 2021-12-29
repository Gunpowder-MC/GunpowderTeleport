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

package io.github.gunpowder.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.builders.Command
import io.github.gunpowder.api.builders.TeleportRequest
import io.github.gunpowder.api.ext.getPermission
import io.github.gunpowder.configs.TeleportConfig
import io.github.gunpowder.ext.center
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import net.minecraft.world.dimension.DimensionType

object SpawnCommand {
    val teleportDelay: Int
        get() = GunpowderMod.instance.registry.getConfig(TeleportConfig::class.java).teleportDelay

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Command.builder(dispatcher) {
            command("spawn") {
                permission("teleport.spawn", 0)
                executes(SpawnCommand::execute)
            }
        }
    }

    fun execute(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player

        val delay = context.source.player.getPermission("teleport.spawn.timeout.[int]", teleportDelay)

        TeleportRequest.builder {
            player(player)
            dimension(World.OVERWORLD)
            destination(GunpowderMod.instance.server.getWorld(World.OVERWORLD)!!.spawnPos.center())
        }.execute(delay.toLong())

        if (delay > 0) {
            context.source.sendFeedback(LiteralText("Teleporting in $delay seconds..."), false)
        }

        return 1
    }
}

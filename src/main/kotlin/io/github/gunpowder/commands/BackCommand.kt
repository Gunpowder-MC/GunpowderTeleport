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
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*

object BackCommand {
    data class LastPosition(
            val position: Vec3d,
            val dimension: World,
            val facing: Vec2f
    )

    val lastPosition = mutableMapOf<UUID, LastPosition>()

    val teleportDelay: Int
        get() = GunpowderMod.instance.registry.getConfig(TeleportConfig::class.java).teleportDelay

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Command.builder(dispatcher) {
            command("back") {
                permission("teleport.back", 0)
                executes(BackCommand::execute)
            }
        }
    }

    private fun execute(context: CommandContext<ServerCommandSource>): Int {
        val id = context.source.player.uuid
        if (lastPosition.containsKey(id)) {
            val p = lastPosition[id]!!
            lastPosition.remove(id)

            context.source.sendFeedback(LiteralText("Teleporting to previous location"), false)

            val delay = context.source.player.getPermission("teleport.back.timeout.[int]", teleportDelay)

            val request = TeleportRequest.builder {
                player(context.source.player)
                destination(p.position)
                facing(p.facing)
                dimension(p.dimension)
            }

            if (delay > 0) {
                context.source.sendFeedback(LiteralText("Teleporting in $delay seconds..."), false)
            }

            request.execute(delay.toLong())

            return 1
        }

        context.source.sendFeedback(LiteralText("No known last location"), false)
        return -1
    }
}

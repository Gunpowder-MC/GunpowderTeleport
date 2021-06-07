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
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.builders.Command
import io.github.gunpowder.api.builders.TeleportRequest
import io.github.gunpowder.api.builders.Text
import io.github.gunpowder.configs.TeleportConfig
import io.github.gunpowder.entities.StoredWarp
import io.github.gunpowder.ext.center
import io.github.gunpowder.modelhandlers.WarpHandler
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3i
import java.util.concurrent.CompletableFuture

object WarpCommand {
    val handler by lazy {
        WarpHandler
    }
    val teleportDelay: Int
        get() = GunpowderMod.instance.registry.getConfig(TeleportConfig::class.java).teleportDelay

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Command.builder(dispatcher) {
            command("warp") {
                literal("list") {
                    executes(WarpCommand::executeList)
                }
                literal("set") {
                    requires {
                        it.hasPermissionLevel(4)
                    }
                    argument("warp", StringArgumentType.greedyString()) {
                        executes(WarpCommand::executeSet)
                    }
                }
                argument("warp", StringArgumentType.greedyString()) {
                    executes(WarpCommand::execute)
                    suggests(WarpCommand::suggestWarps)
                }
            }

            command("setwarp") {
                requires {
                    it.hasPermissionLevel(4)
                }

                argument("warp", StringArgumentType.greedyString()) {
                    executes(WarpCommand::executeSet)
                }
            }

            command("delwarp") {
                requires {
                    it.hasPermissionLevel(4)
                }

                argument("warp", StringArgumentType.greedyString()) {
                    suggests(WarpCommand::suggestWarps)
                    executes(WarpCommand::executeDel)
                }
            }
        }
    }

    fun execute(context: CommandContext<ServerCommandSource>): Int {
        val warpName = StringArgumentType.getString(context, "warp")
        val warp = handler.getWarp(warpName)

        if (warp == null) {
            context.source.player.sendMessage(LiteralText("No such warp: '$warp'"), false)
            return -1
        }

        val player = context.source.player

        TeleportRequest.builder {
            player(player)
            destination(warp.location.center())
            dimension(warp.dimension)
        }.execute(teleportDelay.toLong())

        if (teleportDelay > 0) {
            context.source.sendFeedback(LiteralText("Teleporting in ${teleportDelay.toLong()} seconds..."), false)
        }

        return 1
    }

    fun executeList(context: CommandContext<ServerCommandSource>): Int {
        val warps = (handler as WarpHandler).cache
        val text = Text.builder {
            text("Warps:")
            warps.forEach {
                text("\n - ")
                text(it.key) {
                    onClickCommand("/warp ${it.key}")
                    color(Formatting.YELLOW)
                    onHoverText("/warp ${it.key}")
                }
            }
        }
        context.source.player.sendMessage(text, false)
        return 1
    }

    fun executeSet(context: CommandContext<ServerCommandSource>): Int {
        val warp = StringArgumentType.getString(context, "warp")
        val player = context.source.player
        if (handler.newWarp(
                        StoredWarp(
                                warp,
                                Vec3i(player.pos.x, player.pos.y, player.pos.z),
                                player.world.registryKey.value
                        )
                )) {
            player.sendMessage(LiteralText("Warp '$warp' set"), false)
            return 1
        }
        player.sendMessage(LiteralText("Warp '$warp' already exists"), false)
        return -1
    }

    fun executeDel(context: CommandContext<ServerCommandSource>): Int {
        val warp = StringArgumentType.getString(context, "warp")
        if (handler.delWarp(warp)) {
            context.source.player.sendMessage(LiteralText("Warp '$warp' deleted"), false)
            return 1
        }
        context.source.player.sendMessage(LiteralText("No such warp: '$warp'"), false)
        return -1
    }

    fun suggestWarps(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val homes = (handler as WarpHandler).cache
        // TODO: Match input
        return CommandSource.suggestMatching(homes.map { it.key }, builder)
    }
}

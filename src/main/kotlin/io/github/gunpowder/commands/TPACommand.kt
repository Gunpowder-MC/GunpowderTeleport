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
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.builders.Command
import io.github.gunpowder.api.builders.TeleportRequest
import io.github.gunpowder.api.builders.Text
import io.github.gunpowder.api.ext.getPermission
import io.github.gunpowder.configs.TeleportConfig
import io.github.gunpowder.entities.TPACache
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import java.util.concurrent.CompletableFuture

object TPACommand {
    val config: TeleportConfig
        get() = GunpowderMod.instance.registry.getConfig(TeleportConfig::class.java)

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Command.builder(dispatcher) {
            command("tpa") {
                permission("teleport.tpa", 0)
                argument("user", EntityArgumentType.player()) {
                    executes(TPACommand::executeTpa)
                }
            }

            command("tpahere") {
                permission("teleport.tpahere", 0)
                argument("user", EntityArgumentType.player()) {
                    executes(TPACommand::executeTpahere)
                }
            }

            command("tpaccept") {
                permission("teleport.tpaccept", 0)
                executes {
                    executeTpaccept(it, true)
                }
                argument("user", EntityArgumentType.player()) {
                    suggests(TPACommand::suggestTpaResponse)
                    executes {
                        executeTpaccept(it, false)
                    }
                }
            }

            command("tpdeny") {
                permission("teleport.tpdeny", 0)
                executes {
                    executeTpdeny(it, true)
                }
                argument("user", EntityArgumentType.player()) {
                    suggests(TPACommand::suggestTpaResponse)
                    executes {
                        executeTpdeny(it, false)
                    }
                }
            }
        }
    }

    fun executeTpa(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player
        val target = EntityArgumentType.getPlayer(context, "user")

        if (!TPACache.requestTpa(player, target) {
                    player.sendMessage(LiteralText("TPA timed out"), false)
                }) {
            player.sendMessage(LiteralText("Please specify a user"), false)
            return -1
        }

        player.sendMessage(LiteralText("Requested TPA"), false)
        target.sendMessage(Text.builder {
            text(player.gameProfile.name) {
                color(Formatting.RED)
            }
            text(" has requested to teleport to you. To accept, type ")
            text("/tpaccept") {
                color(Formatting.YELLOW)
                onClickCommand("/tpaccept ${player.displayName.asString()}")
                onHoverText("/tpaccept ${player.displayName.asString()}")
            }
            text(", to deny, type ")
            text("/tpdeny") {
                color(Formatting.YELLOW)
                onClickCommand("/tpdeny ${player.displayName.asString()}")
                onHoverText("/tpdeny ${player.displayName.asString()}")
            }
            text(". This request will time out in ${config.tpaTimeout} seconds.")
        }, false)
        return 1
    }

    fun executeTpahere(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player
        val target = EntityArgumentType.getPlayer(context, "user")
        if (!TPACache.requestTpaHere(player, target) {
                    player.sendMessage(LiteralText("TPA timed out"), false)
                }) {
            player.sendMessage(LiteralText("Please specify a user"), false)
            return -1
        }
        player.sendMessage(LiteralText("Requested TPA"), false)
        target.sendMessage(Text.builder {
            text(player.gameProfile.name) {
                color(Formatting.RED)
            }
            text(" has requested you teleport to them. To accept, type ")
            text("/tpaccept") {
                color(Formatting.YELLOW)
                onClickCommand("/tpaccept ${player.displayName.asString()}")
                onHoverText("/tpaccept ${player.displayName.asString()}")
            }
            text(", to deny, type ")
            text("/tpdeny") {
                color(Formatting.YELLOW)
                onClickCommand("/tpdeny ${player.displayName.asString()}")
                onHoverText("/tpdeny ${player.displayName.asString()}")
            }
            text(". This request will time out in ${config.tpaTimeout} seconds.")
        }, false)
        return 1
    }

    fun suggestTpaResponse(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        TPACache.cache.forEach {
            if (it.toPlayer(context.source.player)) {
                builder.suggest(it.requester().gameProfile.name)
            }
        }
        return builder.buildFuture()
    }

    fun executeTpaccept(context: CommandContext<ServerCommandSource>, findSource: Boolean): Int {
        TPACache.closeTpa(if (findSource) null else EntityArgumentType.getPlayer(context, "user"), context.source.player) {
            val delay = context.source.player.getPermission("teleport.tpa.timeout.[int]", config.teleportDelay)

            TeleportRequest.builder {
                player(it.teleportingEntity)
                destination(it.targetLocationEntity.pos)
                dimension(it.targetLocationEntity.world)
            }.execute(delay.toLong())
            it.requester().sendMessage(LiteralText("TPA accepted"), false)
            if (delay > 0) {
                it.teleportingEntity.sendMessage(LiteralText("Teleporting in $delay seconds..."), false)
            }
        }
        return 1
    }

    fun executeTpdeny(context: CommandContext<ServerCommandSource>, findSource: Boolean): Int {
        TPACache.closeTpa(if (findSource) null else EntityArgumentType.getPlayer(context, "user"), context.source.player) {
            it.requester().sendMessage(LiteralText("TPA denied"), false)
        }
        return 1
    }
}

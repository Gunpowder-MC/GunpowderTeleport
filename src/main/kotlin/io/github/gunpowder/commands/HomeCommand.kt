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
import io.github.gunpowder.api.ext.getPermission
import io.github.gunpowder.configs.TeleportConfig
import io.github.gunpowder.entities.StoredHome
import io.github.gunpowder.ext.center
import io.github.gunpowder.modelhandlers.HomeHandler
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.util.concurrent.CompletableFuture

object HomeCommand {
    private val handler by lazy {
        HomeHandler
    }

    private val teleportDelay
        get() = GunpowderMod.instance.registry.getConfig(TeleportConfig::class.java).teleportDelay

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Command.builder(dispatcher) {
            command("home") {
                permission("teleport.home", 0)
                executes(HomeCommand::execute)

                literal("list") {
                    permission("teleport.home.list", 0)
                    executes(HomeCommand::executeList)
                }

                literal("set") {
                    permission("teleport.home.set", 0)
                    executes(HomeCommand::executeSet)

                    argument("home", StringArgumentType.greedyString()) {
                        executes(HomeCommand::executeSetTarget)
                    }
                }

                argument("home", StringArgumentType.greedyString()) {
                    suggests(HomeCommand::suggestHomes)
                    executes(HomeCommand::executeTarget)
                }
            }

            command("delhome") {
                permission("teleport.home.set", 0)
                executes(HomeCommand::executeDel)

                argument("home", StringArgumentType.greedyString()) {
                    suggests(HomeCommand::suggestHomes)
                    executes(HomeCommand::executeDelTarget)
                }
            }

            command("sethome") {
                permission("teleport.home.set", 0)
                executes(HomeCommand::executeSet)

                argument("home", StringArgumentType.greedyString()) {
                    executes(HomeCommand::executeSetTarget)
                }
            }
        }
    }

    fun execute(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player
        val home = handler.getHome(player.uuid, "home")
        if (home == null) {
            player.sendMessage(LiteralText("No such home: 'home'"), false)
            return -1
        }

        val delay = player.getPermission("teleport.home.timeout.[int]", teleportDelay)

        TeleportRequest.builder {
            player(player)
            dimension(home.dimension)
            destination(home.location.center())
        }.execute(delay.toLong())

        if (delay > 0) {
            context.source.sendFeedback(LiteralText("Teleporting in $delay seconds..."), false)
        }

        return 1
    }

    fun executeTarget(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player
        val homeName = StringArgumentType.getString(context, "home")
        val home = handler.getHome(player.uuid, homeName)
        if (home == null) {
            player.sendMessage(LiteralText("No such home: '${homeName}'"), false)
            return -1
        }

        val delay = player.getPermission("teleport.home.timeout.[int]", teleportDelay)

        TeleportRequest.builder {
            player(player)
            dimension(home.dimension)
            destination(home.location.center())
        }.execute(delay.toLong())

        if (delay > 0) {
            context.source.sendFeedback(LiteralText("Teleporting in $delay seconds..."), false)
        }

        return 1
    }

    fun executeList(context: CommandContext<ServerCommandSource>): Int {
        val homes = handler.getHomes(context.source.player.uuid)
        val text = Text.builder {
            text("Homes:")
            homes.forEach {
                text("\n - ")
                text(it.key) {
                    onClickCommand("/home ${it.key}")
                    color(Formatting.YELLOW)
                    onHoverText("/home ${it.key}")
                }
            }
        }
        context.source.player.sendMessage(text, false)
        return 1
    }

    fun executeSet(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player
        val err = handler.newHome(
                        StoredHome(
                                player.uuid,
                                "home",
                                Vec3i(player.pos.x, player.pos.y, player.pos.z),
                                player.world.registryKey.value
                        )
                )
        if (err == "") {
            player.sendMessage(LiteralText("Home 'home' set"), false)
            return 1
        }
        player.sendMessage(LiteralText(err), false)
        return -1
    }

    fun executeSetTarget(context: CommandContext<ServerCommandSource>): Int {
        val home = StringArgumentType.getString(context, "home")
        val player = context.source.player
        val err = handler.newHome(
                        StoredHome(
                                player.uuid,
                                home,
                                Vec3i(player.pos.x, player.pos.y, player.pos.z),
                                player.world.registryKey.value
                        )
                )
        if (err == "") {
            player.sendMessage(LiteralText("Home '$home' set"), false)
            return 1
        }
        player.sendMessage(LiteralText(err), false)
        return -1
    }

    fun executeDel(context: CommandContext<ServerCommandSource>): Int {
        if (handler.delHome(context.source.player.uuid, "home")) {
            context.source.player.sendMessage(LiteralText("Home 'home' deleted"), false)
            return 1
        }
        context.source.player.sendMessage(LiteralText("No such home: 'home'"), false)
        return -1
    }

    fun executeDelTarget(context: CommandContext<ServerCommandSource>): Int {
        val home = StringArgumentType.getString(context, "home")
        if (handler.delHome(context.source.player.uuid, home)) {
            context.source.player.sendMessage(LiteralText("Home '$home' deleted"), false)
            return 1
        }
        context.source.player.sendMessage(LiteralText("No such home: '$home'"), false)
        return -1
    }

    fun suggestHomes(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        // TODO: Autocomplete based on what we have
        val homes = handler.getHomes(context.source.player.uuid)
        return CommandSource.suggestMatching(homes.map { it.key }, builder)
    }
}

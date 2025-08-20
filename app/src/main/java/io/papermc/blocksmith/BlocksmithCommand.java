package io.papermc.blocksmith;


import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;


public class BlocksmithCommand {
  private static Location waitingRoomLocation = new Location(Bukkit.getWorld("superflat-world"), -40, -58, 186);
  private static Location lobby = new Location(Bukkit.getWorld("lobby"), -65, 91, 123);

  public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
    return Commands.literal(commandName)
      .requires(sender -> sender.getSender().hasPermission("permission.blocksmith.admin"))
      .then(waitingRoomCommand())
      .then(lobbyCommand())
      .build();
  }

  private static LiteralArgumentBuilder<CommandSourceStack> waitingRoomCommand() {
    return Commands.literal("waiting-room")
      .executes(ctx -> {
        Bukkit.getOnlinePlayers().stream()
          .filter(player -> !player.hasPermission("permission.blocksmith.admin"))
          .forEach(player -> {
            player.sendMessage(Component.text("sending you to the waiting room..."));
            player.teleport(waitingRoomLocation);
            player.setGameMode(GameMode.ADVENTURE);
          });
        return Command.SINGLE_SUCCESS;
      });
  }

  private static LiteralArgumentBuilder<CommandSourceStack> lobbyCommand() {
    return Commands.literal("lobby")
      .executes(ctx -> {
        Bukkit.getOnlinePlayers().stream()
          .filter(player -> !player.hasPermission("permission.blocksmith.admin"))
          .forEach(player -> {
            player.sendMessage(Component.text("sending you to the lobby..."));
            player.teleport(lobby);
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
          });
        return Command.SINGLE_SUCCESS;
      });
  }
}
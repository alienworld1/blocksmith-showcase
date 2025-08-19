package io.papermc.blocksmith;

import org.bukkit.command.CommandSender;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.Command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;

public class BlocksmithCommand {

  public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
    return Commands.literal(commandName)
      .executes(ctx -> {
        final CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("hi, " + sender.getName() + " time to chill!"));
        return Command.SINGLE_SUCCESS;
      })
      .build();
    }
  }
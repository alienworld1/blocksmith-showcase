package io.papermc.blocksmith;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class NickCommand {
  public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
    return Commands.literal(commandName)
      .then(Commands.argument("nick_name", StringArgumentType.string())
        .executes(ctx -> {
          String newNickName = ctx.getArgument("nick_name", String.class);
          // Handle the nickname change logic here

          CommandSender sender = ctx.getSource().getSender();
          NamedTextColor nickColor = NamedTextColor.GREEN; // Default color

          if (sender.hasPermission("permission.blocksmith.admin")) {
            nickColor = NamedTextColor.GOLD;
          } 

          Entity executor = ctx.getSource().getExecutor();

          if (!(executor instanceof Player player)) {
            sender.sendPlainMessage("only players can nick!");
            return Command.SINGLE_SUCCESS;
          }

          player.displayName(Component.text(newNickName, nickColor));
          player.playerListName(Component.text(newNickName, nickColor));

          return Command.SINGLE_SUCCESS;
        })
      )
      .build();
  }
}

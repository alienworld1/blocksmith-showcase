package io.papermc.blocksmith.blocksmithcommands;

import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.N;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.blocksmith.database.DatabaseManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Plot {
  private static int MaxPlots = 10;

  public static LiteralArgumentBuilder<CommandSourceStack> createCommand(final String commandName) {
    return Commands.literal(commandName)
    .then(setPlot())
    .then(teleportToPlot());
  }

  private static LiteralArgumentBuilder<CommandSourceStack> setPlot() {
    return Commands.literal("set")
      .then(
        Commands.argument("plot_id", IntegerArgumentType.integer(1, MaxPlots))
        .then(Commands.argument("plot_spawn_location", ArgumentTypes.blockPosition())
        .executes(ctx -> {
          int plotId = ctx.getArgument("plot_id", int.class);
          final BlockPositionResolver blockPositionResolver = ctx.getArgument("plot_spawn_location", BlockPositionResolver.class);
          final BlockPosition spawnLocation = blockPositionResolver.resolve(ctx.getSource());

          try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.setPlot(plotId, spawnLocation.blockX(), spawnLocation.blockY(), spawnLocation.blockZ());
          } catch (SQLException e) {
            ctx.getSource().getSender().sendMessage(Component.text("error creating plot: " + e.getMessage()));
            return Command.SINGLE_SUCCESS;
          }

          ctx.getSource().getSender().sendMessage(Component.text("plot " + plotId + " created successfully"));
          return Command.SINGLE_SUCCESS;
        })
        )
      );
  }

  private static LiteralArgumentBuilder<CommandSourceStack> teleportToPlot() {
    return Commands.literal("teleport")
      .then(
        Commands.argument("plot_id", IntegerArgumentType.integer(1, MaxPlots))
          .executes(ctx -> {
            int plotId = ctx.getArgument("plot_id", int.class);
            try {
              DatabaseManager databaseManager = DatabaseManager.getInstance();
              Location spawnLocation = databaseManager.getSpawnLocationOfPlot(plotId);
              if (spawnLocation != null) {
                CommandSender sender = ctx.getSource().getSender();
                Entity executor = ctx.getSource().getExecutor();

                if (!(executor instanceof Player player)) {
                  sender.sendMessage(Component.text("only players can teleport", NamedTextColor.RED));
                  return Command.SINGLE_SUCCESS;
                }

                player.teleport(spawnLocation);
              } else {
                ctx.getSource().getSender().sendMessage(Component.text("plot " + plotId + " does not exist", NamedTextColor.RED));
              }
            } catch (Exception e) {
              ctx.getSource().getSender().sendMessage(Component.text("error teleporting to plot: " + e.getMessage(), NamedTextColor.RED));
            }
            return Command.SINGLE_SUCCESS;
          })
      );
  }

}

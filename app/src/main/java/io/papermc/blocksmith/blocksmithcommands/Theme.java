package io.papermc.blocksmith.blocksmithcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Theme {
  private String m_name;
  private static String m_default_theme = "default";
  private Plugin plugin = Bukkit.getPluginManager().getPlugin("BlocksmithShowcase");
  private long themeDisplayDelayInTicks = 20;
  private ThemeDisplayTask themeDisplayTask = null;

  public Theme(String name) {
    this.m_name = name;
  }

  public Theme() {
    this.m_name = m_default_theme;
  }

  public String name() {
    return m_name;
  }

  private void setName(String newName) {
    this.m_name = newName;
    this.themeDisplayTask = new ThemeDisplayTask(newName);
  }

  public LiteralArgumentBuilder<CommandSourceStack> createCommand(final String name) {
    return Commands.literal(name)
      .then(themeGet())
      .then(themeSet())
      .then(themeShow())
      .then(themeHide());
  }

  private LiteralArgumentBuilder<CommandSourceStack> themeGet() {
    return Commands.literal("get")
      .executes(ctx -> {
        CommandSender sender = ctx.getSource().getSender();
        if (m_name.equals(m_default_theme)) {
          sender.sendMessage(Component.text("theme not set", NamedTextColor.RED));
        } else {
          sender.sendMessage(Component.text("theme = " + m_name, NamedTextColor.GOLD));
        }
        return Command.SINGLE_SUCCESS;
      });
  }

  private LiteralArgumentBuilder<CommandSourceStack> themeSet() {
    return Commands.literal("set")
      .then(
        Commands.argument("new_theme", StringArgumentType.string())
                .executes(ctx -> {
                  hideTheme();
                    try {
                      String newTheme = ctx.getArgument("new_theme", String.class);
                      setName(newTheme);
                      ctx.getSource().getSender().sendMessage(Component.text("theme set to " + newTheme, NamedTextColor.GREEN));
                      return Command.SINGLE_SUCCESS;
                    } catch (IllegalArgumentException e) {
                      ctx.getSource().getSender().sendMessage(Component.text("Invalid theme name", NamedTextColor.RED));
                    } catch (Exception e) {
                      ctx.getSource().getSender().sendMessage(Component.text("An error occurred while setting the theme: " + e.getMessage(), NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                })
    );
  }

  private LiteralArgumentBuilder<CommandSourceStack> themeShow() {
    return Commands.literal("show")
      .executes(ctx -> {
        if (themeDisplayTask == null) {
          ctx.getSource().getSender().sendMessage(Component.text("theme has not been set", NamedTextColor.RED));
        }

        displayTheme();
        return Command.SINGLE_SUCCESS;
      });
  }

  private LiteralArgumentBuilder<CommandSourceStack> themeHide() {
    return Commands.literal("hide")
      .executes(ctx -> {
        hideTheme();
        return Command.SINGLE_SUCCESS;
      });
  }

  private void displayTheme() {
    try {
      if (themeDisplayTask == null) {
        themeDisplayTask = new ThemeDisplayTask(m_name);
      }
      themeDisplayTask.runTaskTimer(plugin, 0, themeDisplayDelayInTicks);
    } catch (IllegalStateException e) {
      // task was already scheduled, so ignore
    }
  }

  private void hideTheme() {
    if (themeDisplayTask == null) {
      return;
    }
    try {
      themeDisplayTask.cancel();
      themeDisplayTask = null;
    } catch (IllegalStateException e) {
      // task was not scheduled, so just ignore
    }
  }
}

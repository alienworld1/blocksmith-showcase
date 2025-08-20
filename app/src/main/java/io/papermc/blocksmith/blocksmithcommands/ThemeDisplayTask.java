package io.papermc.blocksmith.blocksmithcommands;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ThemeDisplayTask extends BukkitRunnable {
  private String m_theme;

  ThemeDisplayTask(String theme) {
    this.m_theme = theme;
  }

  @Override
  public void run() {
    Bukkit.getOnlinePlayers().forEach(player -> {
      player.sendActionBar(Component.text("Current theme: " + m_theme, NamedTextColor.GOLD, TextDecoration.BOLD));
    });
  }
}

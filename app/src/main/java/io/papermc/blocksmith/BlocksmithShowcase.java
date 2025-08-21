package io.papermc.blocksmith;

import io.papermc.blocksmith.database.DatabaseManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class BlocksmithShowcase extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    
    BreakableCommand.initialize(this.getDataFolder());
    
    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
      commands.registrar().register(BlocksmithCommand.createCommand("blocksmith"), "Blocksmith's showcase admin command");
      commands.registrar().register(BreakableCommand.createCommand("breakable"), "Protect blocks from being broken");
      commands.registrar().register(NickCommand.createCommand("nick"), "Set nickname of a player");
    });

    try {
      DatabaseManager databaseManager = DatabaseManager.getInstance();
      databaseManager.createTables();
      getLogger().info("plugin has been initialized!");
      
    } catch (Exception e) {
      getLogger().severe("Failed to enable BlocksmithShowcase plugin: " + e.getMessage());
    }
  }

  @Override
  public void onDisable() {
    // Save protected blocks when the plugin is disabled
    BreakableCommand.onDisable();
    DatabaseManager databaseManager = DatabaseManager.getInstance();
    try {
      databaseManager.close();
    } catch (SQLException e) {
      getLogger().severe("Failed to close database connection: " + e.getMessage());
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    event.getPlayer().sendMessage(Component.text("Hey, " + event.getPlayer().getName() + "! Welcome to blocksmith's showcase!"));
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    if (BreakableCommand.isBlockProtected(event.getBlock().getLocation())) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(Component.text("you can't break this block!", NamedTextColor.RED));
    }
  }
}
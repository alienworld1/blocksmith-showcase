package io.papermc.blocksmith;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    
    // Initialize the BreakableCommand system
    BreakableCommand.initialize(this.getDataFolder());
    
    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
      commands.registrar().register(BlocksmithCommand.createCommand("blocksmith"), "Blocksmith's showcase admin command");
      commands.registrar().register(BreakableCommand.createCommand("breakable"), "Protect blocks from being broken");
    });
  }

  @Override
  public void onDisable() {
    // Save protected blocks when the plugin is disabled
    BreakableCommand.onDisable();
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
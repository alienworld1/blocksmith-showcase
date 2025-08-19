package io.papermc.blocksmith;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class BlocksmithShowcase extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    MultiverseCoreApi coreApi = MultiverseCoreApi.get();
    this.getComponentLogger().debug(Component.text("Multiverse core api loaded"));
    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
      commands.registrar().register(BlocksmithCommand.createCommand("blocksmith"), "Blocksmith's showcase admin command");
    });
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    event.getPlayer().sendMessage(Component.text("Hey, " + event.getPlayer().getName() + "! Welcome to blocksmith's showcase!"));
  }


}
package io.papermc.blocksmith;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.LocalSession;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

public class BreakableCommand {
  // Chunked storage system for performance with large numbers of blocks
  // Map: world_name -> chunk_key -> set_of_block_positions
  private static final Map<String, Map<Long, Set<Integer>>> protectedBlocks = new ConcurrentHashMap<>();
  private static final Object saveLock = new Object();
  private static File dataFile;
  
  // Initialize storage
  public static void initialize(File pluginDataFolder) {
    dataFile = new File(pluginDataFolder, "protected-blocks.dat");
    loadProtectedBlocks();
  }
  
  // Convert location to chunk key (combines chunk x and z coordinates)
  private static long getChunkKey(int chunkX, int chunkZ) {
    return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
  }
  
  // Convert location to local block position within chunk
  private static int getLocalBlockKey(int x, int y, int z) {
    int localX = x & 15; // x % 16
    int localZ = z & 15; // z % 16
    return (localX << 12) | (y << 4) | localZ; // pack into single int
  }
  
  // Add a block to protection
  private static void addProtectedBlock(Location location) {
    String worldName = location.getWorld().getName();
    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;
    long chunkKey = getChunkKey(chunkX, chunkZ);
    int blockKey = getLocalBlockKey(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    
    protectedBlocks
      .computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
      .computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
      .add(blockKey);
  }
  
  // Remove a block from protection
  private static boolean removeProtectedBlock(Location location) {
    String worldName = location.getWorld().getName();
    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;
    long chunkKey = getChunkKey(chunkX, chunkZ);
    int blockKey = getLocalBlockKey(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    
    Map<Long, Set<Integer>> worldChunks = protectedBlocks.get(worldName);
    if (worldChunks == null) return false;
    
    Set<Integer> chunkBlocks = worldChunks.get(chunkKey);
    if (chunkBlocks == null) return false;
    
    boolean removed = chunkBlocks.remove(blockKey);
    
    // Clean up empty data structures
    if (chunkBlocks.isEmpty()) {
      worldChunks.remove(chunkKey);
      if (worldChunks.isEmpty()) {
        protectedBlocks.remove(worldName);
      }
    }
    
    return removed;
  }
  
  // Check if a block is protected (optimized lookup)
  public static boolean isBlockProtected(Location location) {
    String worldName = location.getWorld().getName();
    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;
    long chunkKey = getChunkKey(chunkX, chunkZ);
    int blockKey = getLocalBlockKey(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    
    Map<Long, Set<Integer>> worldChunks = protectedBlocks.get(worldName);
    if (worldChunks == null) return false;
    
    Set<Integer> chunkBlocks = worldChunks.get(chunkKey);
    if (chunkBlocks == null) return false;
    
    return chunkBlocks.contains(blockKey);
  }
  
  // Save protected blocks to file
  private static void saveProtectedBlocks() {
    synchronized (saveLock) {
      try {
        if (!dataFile.getParentFile().exists()) {
          dataFile.getParentFile().mkdirs();
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
          // Convert to serializable format
          Map<String, Map<Long, Set<Integer>>> serializableData = new HashMap<>();
          for (Map.Entry<String, Map<Long, Set<Integer>>> worldEntry : protectedBlocks.entrySet()) {
            Map<Long, Set<Integer>> worldData = new HashMap<>();
            for (Map.Entry<Long, Set<Integer>> chunkEntry : worldEntry.getValue().entrySet()) {
              worldData.put(chunkEntry.getKey(), new HashSet<>(chunkEntry.getValue()));
            }
            serializableData.put(worldEntry.getKey(), worldData);
          }
          oos.writeObject(serializableData);
        }
      } catch (IOException e) {
        System.err.println("Failed to save protected blocks: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
  
  // load protected blocks from file
  @SuppressWarnings("unchecked")
  private static void loadProtectedBlocks() {
    if (!dataFile.exists()) return;
    
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
      Map<String, Map<Long, Set<Integer>>> loadedData = 
        (Map<String, Map<Long, Set<Integer>>>) ois.readObject();
      
      protectedBlocks.clear();
      for (Map.Entry<String, Map<Long, Set<Integer>>> worldEntry : loadedData.entrySet()) {
        Map<Long, Set<Integer>> worldData = new ConcurrentHashMap<>();
        for (Map.Entry<Long, Set<Integer>> chunkEntry : worldEntry.getValue().entrySet()) {
          Set<Integer> chunkBlocks = ConcurrentHashMap.newKeySet();
          chunkBlocks.addAll(chunkEntry.getValue());
          worldData.put(chunkEntry.getKey(), chunkBlocks);
        }
        protectedBlocks.put(worldEntry.getKey(), worldData);
      }
    } catch (IOException | ClassNotFoundException e) {
      System.err.println("Failed to load protected blocks: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  // Get total count of protected blocks
  public static int getTotalProtectedBlocks() {
    return protectedBlocks.values().stream()
      .mapToInt(worldChunks -> worldChunks.values().stream()
        .mapToInt(Set::size)
        .sum())
      .sum();
  }
  
  public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
    return Commands.literal(commandName)
      .requires(sender -> sender.getSender().hasPermission("permission.blocksmith.admin"))
      .then(protectCommand())
      .then(unprotectCommand())
      .then(listCommand())
      .then(saveCommand())
      .build();
  }
  
  private static LiteralArgumentBuilder<CommandSourceStack> protectCommand() {
    return Commands.literal("protect")
      .executes(ctx -> {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
          ctx.getSource().getSender().sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
          return Command.SINGLE_SUCCESS;
        }
        
        try {
          SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
          LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
          Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
          
          if (region == null) {
            player.sendMessage(Component.text("no worldedit selection", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
          }
          
          int blocksProtected = 0;
          for (BlockVector3 blockVector : region) {
            Location bukkitLocation = new Location(
              player.getWorld(),
              blockVector.x(),
              blockVector.y(),
              blockVector.z()
            );
            addProtectedBlock(bukkitLocation);
            blocksProtected++;
          }
          
          // Auto-save after protecting blocks
          saveProtectedBlocks();
          
          player.sendMessage(Component.text("protected " + blocksProtected + " blocks from being broken", NamedTextColor.GREEN));
          
        } catch (IncompleteRegionException e) {
          player.sendMessage(Component.text("incomplete worldedit selection", NamedTextColor.RED));
        } catch (Exception e) {
          player.sendMessage(Component.text("error: " + e.getMessage(), NamedTextColor.RED));
        }
        
        return Command.SINGLE_SUCCESS;
      });
  }
  
  private static LiteralArgumentBuilder<CommandSourceStack> unprotectCommand() {
    return Commands.literal("unprotect")
      .executes(ctx -> {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
          ctx.getSource().getSender().sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
          return Command.SINGLE_SUCCESS;
        }
        
        try {
          SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
          LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
          Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
          
          if (region == null) {
            player.sendMessage(Component.text("no worldedit selection", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
          }
          
          int blocksUnprotected = 0;
          for (BlockVector3 blockVector : region) {
            Location bukkitLocation = new Location(
              player.getWorld(),
              blockVector.x(),
              blockVector.y(),
              blockVector.z()
            );
            if (removeProtectedBlock(bukkitLocation)) {
              blocksUnprotected++;
            }
          }
          
          // Auto-save after unprotecting blocks
          saveProtectedBlocks();
          
          player.sendMessage(Component.text("unprotected " + blocksUnprotected + " blocks!", NamedTextColor.GREEN));
          
        } catch (IncompleteRegionException e) {
          player.sendMessage(Component.text("incomplete worldedit selection", NamedTextColor.RED));
        } catch (Exception e) {
          player.sendMessage(Component.text("error: " + e.getMessage(), NamedTextColor.RED));
        }
        
        return Command.SINGLE_SUCCESS;
      });
  }
  
  private static LiteralArgumentBuilder<CommandSourceStack> listCommand() {
    return Commands.literal("list")
      .executes(ctx -> {
        int total = getTotalProtectedBlocks();
        ctx.getSource().getSender().sendMessage(Component.text("currently protecting " + total + " blocks", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
      });
  }
  
  private static LiteralArgumentBuilder<CommandSourceStack> saveCommand() {
    return Commands.literal("save")
      .executes(ctx -> {
        saveProtectedBlocks();
        ctx.getSource().getSender().sendMessage(Component.text("saved protected blocks to disk", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
      });
  }
  
  public static void clearAllProtectedBlocks() {
    protectedBlocks.clear();
    saveProtectedBlocks();
  }
  
  // Save data when server shuts down
  public static void onDisable() {
    saveProtectedBlocks();
  }
}

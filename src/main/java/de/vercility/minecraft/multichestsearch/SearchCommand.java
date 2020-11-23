package de.vercility.minecraft.multichestsearch;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import de.kaonashi.minecraft.commons.command.CommandTabCompleter;

public class SearchCommand implements CommandTabCompleter {


    public static Map<UUID, HashSet<Location>> locations = new HashMap<>();


    private final Plugin plugin;

    private YamlConfiguration chestConfig = new YamlConfiguration();
    private File chestFile;

    public static int maxHeight = 10;
    public static int maxRadius = 25; //Anti-lag in case someone exploits this command

    public SearchCommand(Plugin plugin, File cF) {
        this.chestFile = cF;
        this.plugin = plugin;
    }

    /**
     * Execute the /search command
     *
     * @param sender  The entity that issued the command
     * @param command The command object
     * @param label   IDK
     * @param args    Array of arguments passed to the command
     * @return Whether the command has completed successfully or not
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        if (args.length > 2 || args.length == 0) {
            printHelp(player);
            return true;
        }
        // Create a HashSet for the player if its the first time he uses the command or if he cleared his marked chests
        locations.putIfAbsent(player.getUniqueId(), new HashSet<>());
        // Remove all locations in which there no longer is a chest (removed since last command)
        locations.get(player.getUniqueId()).removeIf(Loc -> (Loc.getBlock().getType() != Material.CHEST && Loc.getBlock().getType() != Material.SHULKER_BOX));
        String arg0 = args[0];
        switch (args.length) {
            case 1:
                noArgsCommand(player, arg0);
                return true;
            case 2:
                int radius = parseRadius(args[1], player);
                if (radius == -1) {
                    player.sendMessage("Radius must be greater than 0 and 50 at most");
                    return true;
                }
                radialCommand(player, arg0, radius);
                return true;
            default:
                printHelp(player);
                return true;
        }
    }


    /**
     * Prints all valid commands
     *
     * @param player Player who issued the command
     */
    private void printHelp(Player player) {
        player.sendMessage("/search <Item Name> - Search for item in all marked chests. Ignores letter case. Uses quotation marks to only print exact matches e.g /search \"Stone\"");
        player.sendMessage("/search <Item Name> <radius> - Only search chests within set radius");
        player.sendMessage("/search clear - Unmark all chests");
        player.sendMessage("/search remove - Next clicked chest will be unmarked");
        player.sendMessage("/search mark <radius> - Mark all chests within set radius (horizontally, up to " + maxHeight + " Blocks up/down. Max radius is " + maxRadius + ")");
    }


    /**
     * Parse command radius to int, or print command help
     *
     * @param arg    Argument passed to the command
     * @param player Player who issued the command
     * @return Arg parsed as int if valid integer and smaller than maxRadius, -1 else.
     */
    private int parseRadius(String arg, Player player) {
        int radius = -1;
        try {
            radius = Integer.parseInt(arg);
            if (radius <= 0 || radius > this.maxRadius) {
                return -1;
            }
        } catch (NumberFormatException e) {
            radius = -1;
        }
        return radius;
    }


    /**
     * Execute any radial command
     *
     * @param player Player who issued the command
     * @param arg0   The command
     * @param radius Radius within which the command is to be executed
     */
    private void radialCommand(Player player, String arg0, int radius) {
        switch (arg0) {
            case "mark":
                radialMark(player, radius);
                return;
            case "unmark":
                radialUnmark(player, radius);
                return;
            default:
                searchChests(player, arg0, radius);
        }
    }

    /**
     * Execute any command without arguments
     *
     * @param player Player who issued the command
     * @param arg0   The Command
     */
    private void noArgsCommand(Player player, String arg0) {
        switch (arg0) {
            case "clear":
                locations.remove(player.getUniqueId());
                player.sendMessage("Cleared marked chests");
                return;
            case "remove":
                MultiChestSearchListener.removeNext.put(player.getUniqueId(),true);
                player.sendMessage("Rightclick a chest with a chest marker to unmark it");
                return;
            case "mark":
            case "unmark":
                player.sendMessage("You need to specify a radius");
                return;
            default:
                searchChests(player, arg0, -1);
        }
    }

    /**
     * Marks all chest within given radius
     *
     * @param player Player who issued the command
     * @param radius The radius within chests are to be marked
     */
    private void radialMark(Player player, int radius) {
        int amount = 0;
        final Location playerLocation = player.getLocation();
        final Location center = new Location(player.getWorld(), playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ(), 0, 0);
        final UUID uniqueId = player.getUniqueId();
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -this.maxHeight; z < this.maxHeight; z++) {
                    Vector temp = new Vector(x, y, z);
                    if (temp.length() > radius) {
                        continue;
                    }
                    Location added = center.clone().add(temp);
                    if (player.getWorld().getBlockAt(added).getType() != Material.CHEST && player.getWorld().getBlockAt(added).getType() != Material.SHULKER_BOX) {
                        continue;
                    }
                    if (locations.get(uniqueId).add(added)) {
                        amount++;
                    }
                }
            }
        }
        player.sendMessage(amount + " chests have been marked");
    }

    /**
     * Unmarks all chests within a given radius
     *
     * @param player Player who issued the command
     * @param radius The radius within chests are to be unmarked
     */
    private void radialUnmark(Player player, int radius) {
        HashSet<Location> playerLocs = locations.getOrDefault(player.getUniqueId(), new HashSet<>());
        int length = playerLocs.size();
        if (length == 0) {
            player.sendMessage("You haven't marked any chests to search");
            return;
        }
        playerLocs.removeIf(Loc -> player.getLocation().distance(Loc) <= radius || (Loc.getBlock().getType() != Material.CHEST && Loc.getBlock().getType() != Material.SHULKER_BOX));
        player.sendMessage(length - playerLocs.size() + " chests have been unmarked");
    }

    /**
     * Search for a given item in all marked chests
     *
     * @param player Player who issued the command
     * @param itemId The item to be searched
     * @param radius The radius in which the item is to be searched. Ignored if -1
     */
    private void searchChests(Player player, String itemId, int radius) {
        UUID playerId = player.getUniqueId();
        boolean foundAny = false;
        Map<String, Integer> enderResults = new HashMap<>();
        iterateChest(itemId, enderResults, player.getEnderChest().getContents(), false);
        if (!enderResults.isEmpty()) {
            foundAny = true;
            for (String s : enderResults.keySet()) {
                if (s.startsWith("$")) {
                    player.sendMessage(enderResults.get(s) + " " + s.replace("$", "") + " in shulker Box within ender chest");
                    continue;
                }
                player.sendMessage(enderResults.get(s) + " " + s + " in ender chest");
            }
        }
        if (!locations.containsKey(playerId) || locations.get(playerId).isEmpty()) {
            player.sendMessage("You haven't marked any chests or shulker boxes to search");
            return;
        }
        for (Location l : locations.get(playerId)) {
            Map<String, Integer> results = new HashMap<>();
            if (radius != -1 && l.distance(player.getLocation()) > radius) {
                continue;
            }
            Container b = (Container) l.getBlock().getState();
            ItemStack[] contents = b.getInventory().getContents();
            iterateChest(itemId, results, contents, false);
            if (!results.isEmpty()) {
                foundAny = true;
                Location l2 = l.clone().add(new Vector(0.5, 2, 0.5));

                BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                    l2.getWorld().spawnParticle(Particle.COMPOSTER, l2, 10, 0.25, 0.25, 0.25, 0);
                }, 0, 20);
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    bukkitTask.cancel();
                }, 20 * 3);
                for (String s : results.keySet()) {
                    if (s.startsWith("$")) {
                        player.sendMessage(results.get(s) + " " + s.replace("$", "") + " in shulker box within chest" + " at x:" + l.getX() + " y: " + l.getY() + " z:" + l.getZ() + " in " + l.getWorld().getEnvironment());
                        continue;
                    }
                    player.sendMessage(results.get(s) + " " + s + " at x:" + l.getX() + " y: " + l.getY() + " z:" + l.getZ() + " in " + l.getWorld().getEnvironment());
                }
            }
        }
        if (!foundAny) {
            player.sendMessage("No items found");
        }
    }

    private void iterateChest(String itemId, Map<String, Integer> results, ItemStack[] contents, boolean nested) {
        for (ItemStack is : contents) {
            if (is == null) {
                continue;
            }
            String iSName = is.getType().toString();
            String shulkerSign = "";
            if (nested) {
                shulkerSign = "$";
            }
            if (is.getType() == Material.SHULKER_BOX) {
                iterateChest(itemId, results, ((ShulkerBox) ((BlockStateMeta) is.getItemMeta()).getBlockState()).getInventory().getContents(), true);
            }
            if (itemId.contains("\"")) {
                if (iSName.equalsIgnoreCase(itemId.replaceAll("\"", ""))) {
                    results.merge(shulkerSign + iSName, Integer.valueOf(is.getAmount()), Integer::sum);
                }
            } else if (iSName.toLowerCase().contains(itemId.toLowerCase())) {
                results.merge(shulkerSign + iSName, Integer.valueOf(is.getAmount()), Integer::sum);
            }
            
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getCommand() {
        return "search";
    }

    public void loadConfig(){
        if(!this.chestFile.exists()) {
            return;
        }
        try {
            this.chestConfig.load(this.chestFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        for (String key : this.chestConfig.getKeys(false)) {
            locations.put(UUID.fromString(key), (HashSet<Location>) this.chestConfig.get(key));
        }
    }

    public void saveConfig() {
        for(UUID player : locations.keySet()){
            this.chestConfig.set(player.toString(),locations.get(player));
        }
        try {
            this.chestConfig.save(this.chestFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

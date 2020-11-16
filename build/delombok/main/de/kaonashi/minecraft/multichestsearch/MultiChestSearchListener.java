package de.kaonashi.minecraft.multichestsearch;

import java.io.File;
import java.util.HashSet;
import java.util.UUID;

import de.kaonashi.minecraft.commons.item.ItemHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MultiChestSearchListener implements Listener {

    public static boolean removeNext = false;

    private YamlConfiguration chestConfig;
    private File chestFile;

    public MultiChestSearchListener(YamlConfiguration chestConfig, File chestFile) {
        this.chestConfig = chestConfig;
        this.chestFile = chestFile;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void markChest(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!event.hasBlock() || !event.hasItem()) {
            return;
        }
        ItemStack itemInMainHand = event.getItem();
        if (itemInMainHand == null || !ItemHelper.compareItemsLookalike(itemInMainHand,MultiChestSearchItem.ITEM_STACK)) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }
        // Ignore any event that isn't caused by a right click, where the causing player has a stick in his main hand
        // and the targeted block is a chest
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location location = event.getClickedBlock().getLocation();
        SearchCommand.locations.putIfAbsent(playerId, new HashSet<>());
        //remove command. Unmarks a chest once.
        if (removeNext) {
            removeNext = false;
            if (SearchCommand.locations.get(playerId).remove(location)) {
                player.sendMessage("Unmarked chest");
            } else {
                player.sendMessage("Chest was not marked before");
            }
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }
        // Don't do anything if the chest was already marked to begin with
        if (SearchCommand.locations.get(playerId).contains(location)) {
            return;
        }
        SearchCommand.locations.get(playerId).add(location);
        event.setUseInteractedBlock(Event.Result.DENY); //Don't open the chest
        player.sendMessage("Marked Chest");
    }
}

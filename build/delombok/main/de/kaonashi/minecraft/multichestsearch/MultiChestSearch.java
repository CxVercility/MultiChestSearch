package de.kaonashi.minecraft.multichestsearch;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;

import de.kaonashi.minecraft.commons.command.CommandRegistry;
import de.kaonashi.minecraft.commons.plugin.KaonashiPlugin;

public class MultiChestSearch extends KaonashiPlugin {

    private final File chestFile = new File(getDataFolder(), "chestLocations.yml");
    private YamlConfiguration chestConfig;

    @Override
    protected void onEnableHook() throws Exception {
        this.pm.registerEvents(new MultiChestSearchListener(this.chestConfig, this.chestFile), this);
        CommandRegistry.register(new SearchCommand(this), this);
        this.itemRegistry.register(this,MultiChestSearchItem.ITEM_STACK);
        this.recipeRegistry.register(this,MultiChestSearchItem.class);
        this.advancementRegistry.register(this,MultiChestSearchItem.class);
    }
}

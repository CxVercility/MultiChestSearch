package de.vercility.minecraft.multichestsearch;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;

import de.kaonashi.minecraft.commons.command.CommandRegistry;
import de.kaonashi.minecraft.commons.plugin.KaonashiPlugin;
import org.bukkit.configuration.file.YamlConfigurationOptions;

public class MultiChestSearch extends KaonashiPlugin {
        SearchCommand searchCommand = new SearchCommand(this,new File(getDataFolder(),"chestConfig.yml"));

    @Override
    protected void onEnableHook() throws Exception {
        this.pm.registerEvents(new MultiChestSearchListener(), this);
        this.searchCommand.loadConfig();
        CommandRegistry.register(this.searchCommand, this);
        this.itemRegistry.register(this,MultiChestSearchItem.ITEM_STACK);
        this.recipeRegistry.register(this,MultiChestSearchItem.class);
        this.advancementRegistry.register(this,MultiChestSearchItem.class);
    }

    @Override
    protected void onDisableHook() throws Exception {
    this.searchCommand.saveConfig();
    }
}

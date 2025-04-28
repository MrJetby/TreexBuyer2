package me.jetby.treexbuyer.configurations;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class PriceItemCfg {
    private static FileConfiguration config;
    private static File file;

    public void loadYamlFile(Plugin plugin) {
        file = new File(plugin.getDataFolder(), "priceItem.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("priceItem.yml", true);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration priseItem() {
        return config;
    }

    public void reloadCfg(Plugin plugin) {
        if(!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("priceItem.yml", true);
        }
        try {
            config.load(file);
            Bukkit.getConsoleSender().sendMessage("Конфигурация перезагружена! (priceItem.yml)");
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getConsoleSender().sendMessage("Не удалось перезагрузить конфигурацию! (priceItem.yml)");
        }
    }
    public void saveCfg(Plugin plugin) {
        try {
            File file = new File(plugin.getDataFolder(), "priceItem.yml");
            config.save(file);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Не удалось сохранить файл priceItem.yml", e);
        }
    }
}
